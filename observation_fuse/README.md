# observation_fuse — KTester seed + MutGen mutation feedback

Câu hỏi của observation này:

> Lấy test class do **KTester** sinh làm điểm xuất phát, rồi cho **MutGen** sinh
> thêm test dựa trên phản hồi mutant, thì mutation score có cao hơn KTester
> thuần không?

Đây là **fusion**, không phải so sánh song song. Hai kỹ thuật cùng bồi vào *một*
test class: KTester đưa tri thức dự án để dựng được đối tượng và gọi đúng focal
method; MutGen đưa tín hiệu mutant để biến test đã có thành test *giết được*
mutant.

## Vì sao fuse chứ không chạy MutGen từ đầu

Run cũ `runs/ktester_qwen25_coder_32b` chạy MutGen từ scaffold rỗng ở mức
repo-level. Ví dụ `Base32_decode`: MutGen được 0/85 mutant, KTester được 33/85.
Ở mức repo, model không tự dựng nổi ngữ cảnh khởi tạo đối tượng nên phần lớn
mutant nằm ở trạng thái `NO_COVERAGE`. Seed bằng KTester xoá đúng điểm nghẽn đó,
rồi mutation feedback mới có chỗ phát huy.

## Thiết kế đo

Mỗi focal method chạy trong một workspace cô lập, hai lần chấm PIT trên **cùng**
workspace, **cùng** focal method:

| Arm | Test class được chấm | Ý nghĩa |
| :-- | :-- | :-- |
| **K** | seed KTester, sau khi đã làm xanh | baseline |
| **F** | chính class đó sau khi MutGen thêm test | fusion |

Biến duy nhất thay đổi giữa hai arm là bước sinh thêm test của Stage B. Test gốc
của project bị dời sang `src/ktester-test-disabled` nên không lọt vào classpath;
PIT luôn bị giới hạn đúng class + overload + khoảng dòng của focal method.

Cột `Δ pp` trong báo cáo chính là câu trả lời cho câu hỏi ở đầu file.

## Hai điểm kỹ thuật quyết định tính đúng đắn

1. **Seed phải xanh trước khi sinh.** MutGen nhận/loại một test bằng exit code
   của `mvn -Dtest=<class> test`. Nếu seed đã đỏ sẵn thì *mọi* test mới đều bị
   coi là hỏng và bị revert — kết quả sẽ là 0 test thêm mà không có lỗi nào hiện
   ra. `greenify_seed()` chạy test seed trước, `@Disabled` đúng những method thật
   sự fail (ghi lại đầy đủ trong summary), rồi mới sinh. Seed không compile được
   thì báo `SEED_UNCOMPILABLE` chứ không âm thầm bỏ qua.

2. **`SURVIVED` và `NO_COVERAGE` cần hai loại test khác nhau.** Mutant sống sót
   nghĩa là dòng đó đã chạy nhưng không assertion nào phân biệt được → cần
   assertion sắc hơn. Mutant không được phủ nghĩa là chưa test nào chạm tới →
   cần input mới. `build_fuse_feedback()` tách hai nhóm và nói rõ yêu cầu khác
   nhau, đồng thời liệt kê tên test đã có để model không đặt trùng tên.

## Cấu trúc

```
observation_fuse/
├── config.py                  # đường dẫn, endpoint, tham số — override bằng env
├── pipeline/
│   ├── mutgen_bridge.py       # nơi DUY NHẤT chạm vào nội bộ MUTGEN
│   ├── ktester_stage.py       # Stage A: seed (artifact | generate)
│   └── fuse_stage.py          # Stage B: greenify, feedback, sinh test
├── run_focal.py               # prepare / run / status cho 1 focal method
├── run_batch.py               # inventory / prepare-all / run-all / status-all
├── report_table.py            # bảng kết quả
├── run_fuse.sh                # entry point không cần trông
└── results/<run_name>/        # toàn bộ output
```

`mutgen_bridge.py` import `MUTGEN/scripts/run_ktester_focal.py` như một module
(file đó import-safe: chỉ có hằng số ở module level, `main()` nằm sau guard).
Phần plumbing khó của repo-level — resolve Maven module, chèn plugin PIT, tính
khoảng dòng focal, parse Surefire, chọn JDK theo project — dùng lại nguyên vẹn
thay vì fork ra bản thứ hai rồi lệch nhau.

## Stage A: hai chế độ lấy seed

| `FUSE_KTESTER_SOURCE` | Hành vi | Chi phí |
| :-- | :-- | :-- |
| `artifact` *(mặc định)* | Dùng test class KTester có sẵn trong dataset | 0 token |
| `generate` | Chạy lại pipeline KTester bằng LLM cấu hình được | 4 lần gọi LLM / focal method |

Chế độ `generate` **không** sửa repo KTester. Nó copy `KTester/code` vào
`results/<run>/ktester_runtime/`, vá 5 chỗ Windows-only (dấu `;` trong classpath
JPype và `dependencies.txt`, và `subprocess.run(['cd', ...], shell=True)` vốn
chỉ chạy mỗi lệnh `cd` trên POSIX), sinh `settings.py` trỏ vào endpoint
OpenAI-compatible, rồi gọi thẳng `GenPrompt` / `GenCode` / `Post` của KTester.
Index tri thức được ghép bằng symlink: `json/` + `codegraph/` từ repo KTester,
`lucene/` từ `data/lucene`.

## Cần chuẩn bị gì

**Bắt buộc (cho chế độ mặc định `artifact`):**

```bash
export OLLAMA_SERVER_URL="http://10.148.182.141:11434"   # server qwen của bạn
export OLLAMA_MODEL="qwen2.5-coder:32b"
```

Không cần API key. Ollama bỏ qua key, còn Stage A ở chế độ `artifact` không gọi
LLM. Java 11 và 21 đã có sẵn trong `MUTGEN/.tools/`, Maven lấy từ PATH.

**Chỉ khi chuyển Stage A sang endpoint khác Ollama** (OpenAI, vLLM có auth...):

```bash
export FUSE_KTESTER_SOURCE=generate
export KTESTER_LLM_BASE_URL="https://api.openai.com/v1"
export KTESTER_LLM_API_KEY="<KEY THẬT CỦA BẠN>"    # placeholder sẽ bị từ chối
export KTESTER_LLM_MODEL="gpt-4o-mini"
pip install JPype1==1.6.0                           # chỉ chế độ generate mới cần
```

`config.require_ktester_api_key()` chặn ngay từ đầu nếu endpoint không phải
Ollama mà key vẫn là giá trị mặc định — không để chạy nửa chừng rồi mới lỗi.

## Chạy

```bash
cd /home/hactt13/rs_testing/mutgen_repo/observation_fuse

python3 config.py                                    # xem cấu hình đang hiệu lực
python3 run_batch.py inventory                       # 111 focal method / 10 project

# Smoke 1 task (đã prepare sẵn; seed xanh, 20 mutant)
python3 run_focal.py run --task-id Flat3Map_get
python3 run_focal.py status --task-id Flat3Map_get

# Một project
./run_fuse.sh --project commons-cli

# Toàn bộ, không cần trông
tmux new-session -d -s fuse -c "$PWD" "./run_fuse.sh 2>&1 | tee results/fuse.log"
```

`run-all` bỏ qua task đã `completed`, reset task `running`/`failed` về scaffold
sạch rồi chạy lại, và tự dừng trước khi đĩa xuống dưới `--min-free-gib` (mặc
định 6). Mỗi workspace ~1.7 MB vì `libs/` và `target/` bị loại khi copy và
`target/` bị xoá sau khi task xong (`--keep-build-output` để giữ lại).

## Metric

Mọi metric đo cho **cả hai arm**, scoped đúng focal method: JaCoCo khớp theo
overload (tên method + dòng khai báo nằm trong khoảng focal), PIT lọc theo class
+ overload + khoảng dòng.

### Từ paper KTester

| Ký hiệu | Nghĩa |
| :-- | :-- |
| **CPR** | Compile pass rate — tỉ lệ task mà test class compile được |
| **EPR** | Execution pass rate — tỉ lệ task mà toàn bộ test chạy xanh |
| **LC / BC / IC** | Line / Branch / Instruction coverage của focal method |
| **LCP / BCP / ICP** | Như trên nhưng tính 0 khi suite đỏ |
| **AvTC** | Số test case trung bình |
| **AvT** | Thời gian sinh trung bình (giây) |

Số công bố trong paper KTester đi kèm code (`mb.PAPER_KTESTER`) và hiện thành
một cột riêng trong bảng — mốc tham chiếu, không phải so sánh cặp.

KTester trong code gốc báo *instruction* coverage; adapter của MUTGEN dùng
*line*. Ở đây xuất cả ba (LINE / BRANCH / INSTRUCTION) nên đối chiếu được với
cả hai cách đọc.

### Từ paper MutGen

| Ký hiệu | Nghĩa |
| :-- | :-- |
| **MS** | Mutation score = `detected / total` (chuẩn PIT: KILLED + TIMED_OUT + MEMORY_ERROR) |
| killed / survived / no_coverage | Phân rã trạng thái mutant |
| **Kills/test** | Số mutant giết được trên mỗi test method |
| Theo operator | MS tách theo từng mutation operator của PIT (góc nhìn RQ2) |

Bảng theo operator đọc thẳng tên mutator từ XML của PIT. Script gốc
`analysisPITestReport.py` hỏi LLM để phân loại mutant — không cần thiết và
không tất định, vì PIT đã ghi sẵn tên operator.

### So sánh cặp

Với mỗi focal method: `Δ = F − K` cho mọi metric ở trên, cộng thêm

- `newly_killed` / `lost_kills` — mutant mới bị giết và mutant *mất* kill, kèm
  danh sách chi tiết (dòng, operator, description). Mutant định danh bằng
  `(line, mutator, description, method_description)` vì index PIT xê dịch khi
  test suite đổi.
- **Wilcoxon signed-rank** trên MS, LC, BC + effect size rank-biserial. Dữ liệu
  paired theo thiết kế (cùng focal method, cùng workspace, cùng tập mutant) và
  phân phối hiệu không chuẩn, nên signed-rank là kiểm định phù hợp.

### Một điểm cần đọc cẩn thận: EPR của arm F

Nếu seed KTester có test fail, pipeline `@Disabled` đúng những method đó để PIT
chạy được và để MutGen có tín hiệu accept/reject sạch. Arm F kế thừa các
`@Disabled` ấy, nên nó được báo theo hai cách:

- `execution_pass` — **strict**: `False` nếu có bất kỳ method nào bị disable, tức
  là so ngang với arm K. Đây là cột dùng để tính EPR trong bảng.
- `execution_pass_new_tests_only` — **lenient**: chỉ hỏi phần test mới có xanh không.

Không dùng strict thì EPR của arm F sẽ bị thổi lên chỉ vì ta đã tắt test hỏng
của seed.

## Kết quả

Trong `results/<run_name>/`:

| File | Nội dung |
| :-- | :-- |
| `results_table.md` | 3 bảng: metric paper, theo operator, theo focal method |
| `paper_metrics.json` | Toàn bộ aggregate + kiểm định, dạng máy đọc |
| `batch_status.{json,csv}` | Một dòng mỗi focal method |
| `tasks/<id>/results/summary.json` | Đầy đủ số liệu một task, cả hai arm |
| `tasks/<id>/results/seed_test.java` | Test class KTester trước khi fuse |
| `tasks/<id>/results/fused_test.java` | Sau khi fuse — diff ra phần MutGen thêm |
| `tasks/<id>/results/*_jacoco.xml` | Report JaCoCo từng arm |
| `tasks/<id>/results/*_mutations.xml` | Report PIT từng arm |
| `tasks/<id>/results/mutation_feedback_r*.txt` | Prompt feedback đã gửi model |
| `tasks/<id>/logs/` | Log từng lệnh Maven / JaCoCo / PIT / generation |

```bash
python3 report_table.py --only-completed                 # bảng đầy đủ
python3 report_table.py --only-completed --with-scratch  # thêm cột MutGen-from-scratch
python3 report_table.py --sort-by d_lc_pp                # sắp theo Δ line coverage
```

## Bao nhiêu focal method fuse được

Không phải seed KTester nào cũng dùng được. Đếm từ run MutGen cũ
(`runs/ktester_qwen25_coder_32b`, 90 task đã chấm reference):

| Tình trạng seed | Số task | Fuse được? |
| :-- | --: | :-- |
| Xanh hoàn toàn | 12 | có |
| Đỏ một phần, lọc bớt test là xanh | 52 | có |
| Không compile | 23 | không — `SEED_UNCOMPILABLE` |
| Không có artifact | 3 | không — `SEED_MISSING` |

Tức khoảng **71% fuse được**. Nguyên nhân phổ biến nhất của nhóm không compile
là seed dùng cú pháp mới hơn source level của project — ví dụ
`PatternOptionBuilder_getValueType` sinh switch expression `case 'x' -> ...`
(Java 14+) trong khi `commons-cli` đặt `maven.compiler.source=11`. Đây là tính
chất của artifact KTester, không phải của harness; pipeline phát hiện và báo
`SEED_UNCOMPILABLE` thay vì gán điểm 0.

Các task không fuse được bị loại khỏi aggregate, **không** bị tính là MS = 0 —
gán 0 sẽ kéo tụt baseline KTester và làm phần chênh lệch trông đẹp hơn thực tế.

## Trạng thái hiện tại

- Đường `prepare` và toàn bộ đường đo metric KTester (`measure_arm`) đã chạy
  thật trên `Flat3Map_get`: CPR/EPR pass, LC 100%, BC 75% (24/32 nhánh),
  IC 100% — scoped đúng overload của focal method.
- Đường sinh test của Stage B **chưa chạy** — cần Ollama sống.
- Stage A chế độ `generate` viết đủ nhưng **chưa chạy lần nào**; cần `jpype` và
  index Lucene đầy đủ. Chế độ `artifact` là đường đang dùng.
