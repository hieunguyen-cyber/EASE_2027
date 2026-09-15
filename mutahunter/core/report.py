"""
Module for generating mutation testing reports.
"""

from importlib import resources
from pathlib import Path
from typing import Any, Dict, List, Union

from jinja2 import Environment, FileSystemLoader

from mutahunter.core.db import MutationDatabase
from mutahunter.core.logger import logger
from mutahunter.core.paths import RunPaths

MUTAHUNTER_ASCII = r"""
.  . . . .-. .-. . . . . . . .-. .-. .-. 
|\/| | |  |  |-| |-| | | |\|  |  |-  |(  
'  ` `-'  '  ` ' ' ` `-' ' `  '  `-' ' ' 
"""


class MutantReport:
    """Class for generating mutation testing reports."""

    def __init__(self, db: MutationDatabase) -> None:
        self.paths = RunPaths.default()
        self.paths.ensure()
        self.log_file = self.paths.latest / "coverage.txt"
        self.db = db
        self.template_env = Environment(
            loader=FileSystemLoader(resources.files(__package__).joinpath("templates"))
        )
        assert self.template_env.get_template("report_template.html")
        assert self.template_env.get_template("file_detail_template.html")

    def generate_report(self, total_cost: float, line_rate: float, run_id: int) -> None:
        """
        Generates a comprehensive mutation testing report.

        Args:
            total_cost (float): The total cost of mutation testing.
            line_rate (float): The line coverage rate.
        """
        print(MUTAHUNTER_ASCII)
        data = self.db.get_mutant_summary(run_id)
        self._generate_summary_report(data, total_cost, line_rate)

        # # HTML Report
        file_data = self.db.get_file_data(run_id=run_id)
        # Generate main report
        main_html = self._generate_main_report(data, file_data, total_cost, line_rate)
        self._write_html_report(main_html, "mutation_report.html")
        # Generate detailed file reports
        for file_info in file_data:
            file_html = self._generate_file_report(file_info["id"])
            self._write_html_report(file_html, f"{file_info['id']}.html")

    def _generate_main_report(
        self,
        summary_data: Dict[str, Any],
        file_data: List[Dict[str, Any]],
        total_cost: float,
        line_rate: float,
    ) -> str:
        valid_mutants = (
            summary_data["total_mutants"]
            - summary_data["compile_error_mutants"]
            - summary_data["timeout_mutants"]
        )
        mutation_coverage = (
            f"{summary_data['killed_mutants'] / valid_mutants * 100:.2f}"
            if valid_mutants > 0
            else "0.00"
        )

        template = self.template_env.get_template("report_template.html")
        return template.render(
            line_coverage=f"{line_rate * 100:.2f}",
            mutation_coverage=mutation_coverage,
            total_mutants=summary_data["total_mutants"],
            killed_mutants=summary_data["killed_mutants"],
            survived_mutants=summary_data["survived_mutants"],
            timeout_mutants=summary_data["timeout_mutants"],
            compile_error_mutants=summary_data["compile_error_mutants"],
            total_cost=f"{total_cost:.7f}",
            file_data=file_data,
        )

    def _generate_file_report(self, file_id: str) -> str:
        file_name = self.db.get_source_file_by_id(file_id)
        source_code = self._get_source_code(file_name)
        mutations = self.db.get_file_mutations(file_name)

        source_lines = []
        for i, line in enumerate(source_code.splitlines(), start=1):
            line_mutations = [m for m in mutations if m["line_number"] == i]
            gutter = (
                "survived"
                if any(m["status"] == "SURVIVED" for m in line_mutations)
                else "killed" if line_mutations else ""
            )
            source_lines.append(
                {"code": line, "gutter": gutter, "mutations": line_mutations}
            )

        template = self.template_env.get_template("file_detail_template.html")
        return template.render(file_name=file_name, source_lines=source_lines)

    def _get_source_code(self, file_name: str) -> str:
        return Path(file_name).read_text(encoding="utf-8")

    def _write_html_report(self, html_content: str, filename: str) -> None:
        (self.paths.html / filename).write_text(html_content, encoding="utf-8")
        logger.info(f"HTML report generated: {filename}")

    def _generate_summary_report(
        self, data: Dict[str, Union[int, float]], total_cost: float, line_rate: float
    ) -> None:
        """
        Generates a summary mutation testing report.

        Args:
            total_cost (float): The total cost of mutation testing.
            line_rate (float): The line coverage rate.
        """
        summary_text = self._format_summary(data, total_cost, line_rate)
        self._log_and_write(summary_text)

    def _format_summary(
        self, data: Dict[str, Any], total_cost: float, line_rate: float
    ) -> str:
        """
        Formats the summary data into a string.

        Args:
            data (Dict[str, Any]): Summary data including counts of different mutant statuses.
            total_cost (float): The total cost of mutation testing.
            line_rate (float): The line coverage rate.

        Returns:
            str: Formatted summary report.
        """
        line_coverage = f"{line_rate * 100:.2f}%"
        mutation_coverage = f"{data['mutation_coverage']*100:.2f}%"
        details = [
            f"\n=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=\n",
            "📊 Overall Mutation Coverage 📊",
            f"📈 Line Coverage: {line_coverage} 📈",
            f"🎯 Mutation Coverage: {mutation_coverage} 🎯",
            f"🦠 Total Mutants: {data['total_mutants']} 🦠",
            f"🛡️ Survived Mutants: {data['survived_mutants']} 🛡️",
            f"🗡️ Killed Mutants: {data['killed_mutants']} 🗡️",
            f"🕒 Timeout Mutants: {data['timeout_mutants']} 🕒",
            f"🔥 Compile Error Mutants: {data['compile_error_mutants']} 🔥",
            f"💰 Total Cost: ${total_cost:.5f} USD 💰",
            f"\n=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=\n",
        ]
        return "\n".join(details)

    def _log_and_write(self, text: str) -> None:
        """
        Logs and writes the given text to a file.

        Args:
            text (str): The text to log and write.
        """
        logger.info(text)
        with self.log_file.open("a", encoding="utf-8") as file:
            file.write(text + "\n")
