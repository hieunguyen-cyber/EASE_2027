import json
import os
import subprocess
import pprint
import re
import yaml
from grep_ast import filename_to_lang
import time

from mutahunter.core.analyzer import Analyzer
from mutahunter.core.code_merger import merge_code
from mutahunter.core.coverage_processor import CoverageProcessor
from mutahunter.core.entities.config import UnittestGeneratorLineConfig
from mutahunter.core.error_parser import extract_error_message
from mutahunter.core.logger import logger
from mutahunter.core.prompt_factory import TestGenerationPrompt
from mutahunter.core.router import LLMRouter
from mutahunter.core.utils import FileUtils


class UnittestGenLine:
    def __init__(
        self,
        config: UnittestGeneratorLineConfig,
        coverage_processor: CoverageProcessor,
        analyzer: Analyzer,
        router: LLMRouter,
        prompt: TestGenerationPrompt,
    ):
        self.config = config
        self.coverage_processor = coverage_processor
        self.analyzer = analyzer
        self.router = router

        self.failed_tests = []
        self.num = 0
        self.current_line_coverage_rate = 0.0
        self.prompt = prompt

    def run(self) -> None:
        self.increase_line_coverage()

    def increase_line_coverage(self):
        start_plantime = time.time()
        end_plantime = time.time()
        start_resptime = time.time()
        test_plan = None
        response = self.generate_tests(test_plan)
        end_resptime = time.time()
        print(f"unit_test_gen.py, response for generation: --------/n ${response} ---------with type {type(response)}/n")
        response = response or {}
        if not isinstance(response, dict):
            raise ValueError("The model response must be a YAML mapping")
        new_tests = response.get("new_tests", [])
        print(f"unit_test_gen.py, new tests for generation: --------/n ${new_tests} ---------/n")
        start_valtime = time.time()
        for new_test in new_tests:
            if new_test is None:
                continue
            print(f"unit_test_gen.py, new test for generation: --------/n ${new_test} ---------/n")
            self.validate_unittest(
                new_test,
            )
        end_valtime = time.time()
        print(f"unit_test_gen.py, ${pprint.pformat(self.failed_tests)}/n")
        totalfails = len(self.failed_tests)
        fixfails = 0
        start_fixtime = time.time()
        if self.failed_tests:
            ori_failed_tests = list(self.failed_tests)
            for err_test in ori_failed_tests:
                trytimes = 0
                print(f"unit_test_gen.py, before fix:/n ${pprint.pformat(err_test)}/n")
                while trytimes < 1:
                    self.failed_tests = []
                    trytimes += 1
                    fix_test = []
                    pattern = r"method\s+([a-zA-Z_][a-zA-Z0-9_]*)\s*\([^()]*\)\s+is already"
                    match = re.search(pattern, str(err_test["error_message"]))
                    if match:
                        method_name = match.group(1).strip()
                        new_method_name = "dedul_" + method_name
                        new_text = re.sub(method_name, new_method_name, err_test["code"])
                        fix_test.append({"test_code":new_text})
                    else:
                        fix_response = self.fix_one_failed_tests(err_test)
                        fix_test = (fix_response or {}).get("new_tests", [])
                    if not fix_test or fix_test[0] is None:
                        continue
                    self.validate_unittest(fix_test[0])
                    if len(self.failed_tests)==0:
                        break
                    err_test = self.failed_tests[0]
                if len(self.failed_tests)==0:
                    fixfails += 1
        end_fixtime = time.time()
        print(f"unit_test_gen.py, total fails: {totalfails}, fixed: {fixfails}")
        print(f"unit_test_gen.py, plan time: {end_plantime - start_plantime:.2f} seconds")
        print(f"unit_test_gen.py, response time: {end_resptime - start_resptime:.2f} seconds")
        print(f"unit_test_gen.py, validate time: {end_valtime - start_valtime:.2f} seconds")
        print(f"unit_test_gen.py, fix time: {end_fixtime - start_fixtime:.2f} seconds")
        print(f"unit_test_gen.py, total time: {end_fixtime - start_plantime:.2f} seconds")

    def fix_one_failed_tests(self,errdict) -> dict:
        formjson = [errdict]
        errmsg = errdict["error_message"]
        if "COMPILATION ERROR" in errmsg:
            fixprompt_sysclass = self.prompt.fix_comp_system_prompt
            fixprompt_usrclass = self.prompt.fix_comp_user_prompt
        else:
            fixprompt_sysclass = self.prompt.fix_assert_system_prompt
            fixprompt_usrclass = self.prompt.fix_assert_user_prompt
        try:
            system_prompt = fixprompt_sysclass.render(
                {
                    "test_framework": self.config.test_framework,
                    "language": "Java",
                }
            )
            user_prompt = fixprompt_usrclass.render(
                language="Java",
                test_framework=self.config.test_framework,
                failed_tests=(
                    json.dumps(formjson, indent=2)
                    if formjson
                    else None
                ),
            )
            prompt={"system": system_prompt, "user": user_prompt}
            print(f"unit_test_gen.py, prompt for fix assert: --------/n ${prompt} ---------/n")
            response, _, _ = self.router.generate_response(
                prompt=prompt, streaming=True
            )
            response = self.router.extract_yaml_from_response(response)
            return response
        except Exception as e:
            raise

    def fix_failed_tests(self) -> dict:
        try:
            system_prompt = self.prompt.fix_system_prompt.render(
                {
                    "test_framework": self.config.test_framework,
                    "language": "Java",
                }
            )
            user_prompt = self.prompt.fix_user_prompt.render(
                language="Java",
                test_framework=self.config.test_framework,
                failed_tests=(
                    json.dumps(self.failed_tests, indent=2)
                    if self.failed_tests
                    else None
                ),
            )
            prompt={"system": system_prompt, "user": user_prompt}
            print(f"unit_test_gen.py, prompt for fix: --------/n ${prompt} ---------/n")
            response, _, _ = self.router.generate_response(
                prompt=prompt, streaming=True
            )
            response = self.router.extract_yaml_from_response(response)
            return response
        except Exception as e:
            raise

    def analyze_code(self):
        system_template = self.prompt.analyzer_system_prompt.render()
        src_code = FileUtils.read_file(self.config.source_file_path)
        language = filename_to_lang(self.config.source_file_path)
        source_file_numbered = FileUtils.number_lines(src_code)
        lines_to_cover = self.coverage_processor.file_lines_not_executed.get(
            self.config.source_file_path, []
        )
        test_code = FileUtils.read_file(self.config.test_file_path)
        user_template = self.prompt.analyzer_user_prompt.render(
            {
                "language": language,
                "source_file_name": self.config.source_file_path,
                "source_file_numbered": source_file_numbered,
                "lines_to_cover": lines_to_cover,
                "test_file_name": self.config.test_file_path,
                "test_file": test_code,
                #"test_file": "",
            }
        )
        prompt = {"system": system_template, "user": user_template}
        print(f"unit_test_gen.py, prompt for generation: --------/n ${prompt} ---------/n")
        response, _, _ = self.router.generate_response(
            prompt=prompt, streaming=True
        )
        return self.router.extract_yaml_from_response(response)

    def generate_tests(self, test_plan: dict = None) -> dict:
        try:
            source_code = (
                self.config.source_code_override
                or FileUtils.read_file(self.config.source_file_path)
            )
            test_code = FileUtils.read_file(self.config.test_file_path)
            language = filename_to_lang(self.config.source_file_path)
            system_prompt = self.prompt.test_generator_system_prompt.render(
                {
                    "test_framework": self.config.test_framework,
                    "language": "Java",
                }
            )
            user_prompt = self.prompt.test_generator_user_prompt.render(
                language=language,
                source_code=self._add_line_numbers(source_code),
                source_file_name=self.config.source_file_path,
                test_framework=self.config.test_framework,
                test_file_name=self.config.test_file_path,
                test_file=test_code,
                source_description=self.config.source_description,
                mutation_feedback=self.config.mutation_feedback,
                test_plan=json.dumps(test_plan, indent=2) if test_plan else None,
                failed_tests=(
                    json.dumps(self.failed_tests, indent=2)
                    if self.failed_tests
                    else None
                ),
            )
            prompt={"system": system_prompt, "user": user_prompt}
            print(f"unit_test_gen.py, prompt for generation: --------/n ${prompt} ---------/n")
            response, _, _ = self.router.generate_response(
                prompt=prompt, streaming=True
            )
            response = self.router.extract_yaml_from_response(response)
            return response
        except Exception as e:
            raise

    def _add_line_numbers(self, src_code: str) -> str:
        return "\n".join(
            [f"{i + 1} {line}" for i, line in enumerate(src_code.splitlines())]
        )

    def validate_unittest(
        self,
        generated_unittest: dict,
    ) -> None:
        try:
            new_test_code = generated_unittest.get("test_code", "")
            new_imports_code = generated_unittest.get("new_imports_", "")
            assert (
                new_test_code != ""
            ), "New test code is empty in the generated unittest"
            FileUtils.backup_code(self.config.test_file_path)
            test_file_code = FileUtils.read_file(self.config.test_file_path)
            test_block_nodes = self.analyzer.get_test_nodes(
                source_file_path=self.config.test_file_path
            )
            test_class_block_node = self.analyzer.get_test_class_node(
                source_file_path=self.config.test_file_path
            )
            #print(f"test_block_nodes: {test_block_nodes}")
            if len(test_class_block_node) > 0:
                indent_level = test_class_block_node[0].start_point[1]
                line_number = test_class_block_node[0].end_point[0]
                #print(f"test_class_node: {test_class_block_node}")

                modified_src_code = merge_code(
                    code_to_insert=new_test_code,
                    org_src_code=test_file_code,
                    indent_level=indent_level,
                    line_number=line_number,
                )
                #print(f"new imports: {new_imports_code}")
                #print(f"test file code: {test_file_code}")
                import_nodes = self.analyzer.get_import_nodes(
                    source_file_path=self.config.test_file_path
                )
                import_texts = [test_file_code[node.start_byte:node.end_byte] for node in import_nodes]
                import_insert_line = import_nodes[0].start_point[0]
                if new_imports_code or "import" in str(new_imports_code):
                    for new_import in new_imports_code.splitlines():
                        if new_import not in str(import_texts):
                            modified_src_code = merge_code(
                                code_to_insert=new_import,
                                org_src_code=modified_src_code,
                                indent_level=0,
                                line_number=import_insert_line,
                            )
            if self.analyzer.check_syntax(
                self.config.test_file_path, modified_src_code
            ):
                with open(self.config.test_file_path, "w") as file:
                    file.write(modified_src_code)
                result = subprocess.run(
                    self.config.test_command.split(),
                    capture_output=True,
                    text=True,
                    cwd=os.getcwd(),
                )
                if result.returncode == 0:
                    logger.info(f"Test passed for\n{new_test_code}")
                    return
                else:
                    logger.info(f"Test failed for\n{new_test_code}")
                    self._handle_failed_test(result, new_test_code)
                    FileUtils.revert(self.config.test_file_path)
            else:
                logger.info(f"Test failed (Syntax error) for\n{new_test_code}")
                self.failed_tests.append(
                    {
                        "code": new_test_code,
                        "error_message": "Generated Java test has a syntax error.",
                    }
                )
                FileUtils.revert(self.config.test_file_path)
        except Exception as e:
            logger.info(f"Failed to validate unittest: {e}")
            FileUtils.revert(self.config.test_file_path)
            raise

    def check_line_coverage_increase(self):
        self.coverage_processor.parse_coverage_report()
        new_line_coverage_rate = (
            self.coverage_processor.calculate_line_coverage_rate_for_file(
                self.config.source_file_path
            )
        )
        if new_line_coverage_rate > self.current_line_coverage_rate:
            logger.info(
                f"Line coverage increased from {self.current_line_coverage_rate*100:.2f}% to {new_line_coverage_rate*100:.2f}%"
            )
            self.current_line_coverage_rate = new_line_coverage_rate
            return True
        else:
            return False

    def extract_assertfailed_tests(self,log,start_words,end_words):
        lines = log.split("\n")  # Split log into lines
        start_idx = None
        end_idx = None
        # Find start and end indices
        for i, line in enumerate(lines):
            if start_words in line:
                start_idx = i
            elif end_words in line and start_idx is not None:
                end_idx = i
                break
        # Extract and return the relevant lines
        if start_idx is not None and end_idx is not None:
            return "\n".join(lines[start_idx + 1:end_idx]).strip()
        return ""

    def _handle_failed_test(self, result, test_code):
        lang = self.analyzer.get_language_by_filename(self.config.test_file_path)
        #error_msg = extract_error_message(lang, result.stdout + result.stderr)
        #self.failed_tests.append({"code": test_code, "error_message": error_msg})
        error_msg = extract_error_message(lang, result.stdout + result.stderr)
        error_msg = re.sub(r'\x1B(?:[@-Z\\-_]|\[[0-?]*[ -/]*[@-~])', '', error_msg)
        # HumanEval, MAVEN
        if 'Failed tests' in error_msg:
            # compilation passes, but assertion fails
            error_msg = "Assertion failures: " + self.extract_assertfailed_tests(error_msg,"Failed tests","Tests run")
        else:
            # compilation fails
            error_msg = [line for line in error_msg.split("\n") if "ERROR" in line or "Exception" in line or "failed" in line.lower()]
        # SF110, ANT
        #if 'single:' in error_msg:
        #    error_msg = error_msg.split("single:")[-1]
        #    error_msg = [line for line in error_msg.split("\n") if "DEBUG" not in line and "INFO" not in line and "WARNING" not in line]
        #else:
        #    error_msg = self.extract_assertfailed_tests(error_msg,"compile-evosuite:","BUILD FAILED")
        self.failed_tests.append({"code": test_code, "error_message": error_msg})
