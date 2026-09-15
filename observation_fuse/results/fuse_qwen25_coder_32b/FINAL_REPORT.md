# Kết quả cuối — KTester seed + MutGen mutation feedback

- Model: `qwen2.5-coder:32b` qua Ollama · Dataset: KTester, 111 focal method, 10 project Java
- Chạy: 2026-09-15 12:07 → 15:29 · Stage B tốn 2.20 giờ gọi model
- Arm K = test class KTester · Arm F = chính class đó sau khi MutGen thêm test
- Mọi metric scoped đúng focal method (JaCoCo theo overload, PIT theo class+overload+dòng)

## 1. Kết quả chính (90 task hoàn tất)

| Metric | Arm K | Arm F | Δ | Wilcoxon | ↑/↓ |
| :-- | --: | --: | --: | --: | :-- |
| **Mutation score** | **33.31%** | **43.19%** | **+9.87 pp** | **p=5.1e-09** | 46↑/3↓ |
| Line coverage † | 60.64% | 61.45% | +0.80 pp | p=0.277 | 33↑/17↓ |
| Branch coverage † | 53.27% | 54.26% | +0.99 pp | p=0.236 | 38↑/22↓ |
| Instruction coverage † | 59.28% | 59.97% | +0.68 pp | p=0.316 | 36↑/20↓ |

† Coverage trên toàn tập bị nhiễu — xem mục 2.

**Mutant:** 832 → 1144 bị kill trên 2814 (**+283 mới**, −4 mất)  
**Theo focal method:** fuse thắng **46**, ktester thắng 3, hoà 41  
**Test:** 838 → 1283 (+445); trung bình 9.31 → 14.26/task

## 2. Tập sạch — coverage không bị nhiễu

16 task có seed KTester xanh sẵn, không phải disable test nào. Với các task còn lại, coverage Arm K đo trên seed nguyên bản (test lỗi vẫn phủ code) còn Arm F đo sau khi test lỗi bị `@Disabled`, nên hai vế không cùng cơ sở. Mutation score **không** dính lỗi này: cả hai arm đều chạy PIT sau bước disable.

| Metric | Arm K | Arm F | Δ | Wilcoxon | ↑/↓ |
| :-- | --: | --: | --: | --: | :-- |
| Mutation score | 44.64% | 52.35% | +7.72 pp | p=0.0391 | 7↑/1↓ |
| **Line coverage** | 62.93% | 69.22% | **+6.29 pp** | **p=0.0312** | 6↑/0↓ |
| **Branch coverage** | 54.86% | 63.62% | **+8.76 pp** | **p=0.00391** | 9↑/0↓ |
| **Instruction coverage** | 62.88% | 69.53% | **+6.65 pp** | **p=0.00781** | 8↑/0↓ |

## 3. Theo project

| Project | n | Mutant | KT MS | F MS | Δ MS | +kill |
| :-- | --: | --: | --: | --: | --: | --: |
| batch-processing-gateway | 6 | 157 | 32.48% | 32.48% | +0.00 pp | 0 |
| commons-cli | 1 | 17 | 0.00% | 11.76% | +11.76 pp | 2 |
| commons-codec | 16 | 948 | 24.37% | 41.03% | +16.67 pp | 135 |
| commons-collections | 13 | 392 | 36.22% | 46.43% | +10.20 pp | 36 |
| commons-csv | 6 | 111 | 33.33% | 49.55% | +16.22 pp | 18 |
| datafaker | 6 | 146 | 30.14% | 41.78% | +11.64 pp | 19 |
| gson | 20 | 511 | 34.83% | 41.49% | +6.65 pp | 34 |
| jdom2 | 20 | 480 | 27.29% | 34.79% | +7.50 pp | 33 |
| windward | 2 | 52 | 34.62% | 48.08% | +13.46 pp | 6 |

## 4. Theo mutation operator (RQ2)

| Operator | Mutant | Arm K MS | Arm F MS | Δ |
| :-- | --: | --: | --: | --: |
| RemoveConditionalMutator_EQUAL_ELSE | 1065 | 28.73% | 40.09% | +11.36 pp |
| Math | 572 | 20.10% | 33.04% | +12.94 pp |
| VoidMethodCall | 365 | 16.16% | 21.37% | +5.21 pp |
| ConditionalsBoundary | 191 | 28.27% | 38.22% | +9.95 pp |
| RemoveConditionalMutator_ORDER_ELSE | 191 | 40.31% | 50.79% | +10.48 pp |
| NullReturnVals | 181 | 51.38% | 67.40% | +16.02 pp |
| Increments | 69 | 49.28% | 57.97% | +8.69 pp |
| BooleanFalseReturnVals | 61 | 68.85% | 78.69% | +9.84 pp |
| PrimitiveReturns | 40 | 25.00% | 52.50% | +27.50 pp |
| BooleanTrueReturnVals | 38 | 57.89% | 76.32% | +18.43 pp |
| EmptyObjectReturnVals | 31 | 58.06% | 58.06% | +0.00 pp |

## 5. Phân bố Δ mutation score

| Khoảng | Số task |
| :-- | --: |
| giảm | 3 |
| không đổi | 41 |
| +0–10 pp | 16 |
| +10–25 pp | 18 |
| +25–50 pp | 12 |
| ≥ +50 pp | 3 |

median +3.19 pp · mean +9.87 pp · max +60.00 · min -8.70

## 6. Top 10 cải thiện mạnh nhất

| Focal method | Project | Mutant | KT MS | F MS | Δ | +kill | +test |
| :-- | :-- | --: | --: | --: | --: | --: | --: |
| FakeValuesService_resolveExpression | datafaker | 35 | 2.86% | 62.86% | +60.00 pp | 18 | 2 |
| DoubleMetaphone_doubleMetaphone | commons-codec | 48 | 35.42% | 89.58% | +54.17 pp | 14 | 21 |
| XPathHelper_getSingleStep | jdom2 | 22 | 0.0% | 50.0% | +50.00 pp | 11 | 7 |
| QuotedPrintableCodec_encodeQuotedPrintable | commons-codec | 42 | 2.38% | 42.86% | +40.48 pp | 17 | 5 |
| Flat3Map_remove | commons-collections | 36 | 47.22% | 86.11% | +38.89 pp | 12 | 10 |
| ColognePhonetic_colognePhonetic | commons-codec | 46 | 41.3% | 78.26% | +36.96 pp | 12 | 21 |
| MurmurHash3_hash128x64Internal | commons-codec | 92 | 0.0% | 36.96% | +36.96 pp | 34 | 10 |
| CSVParser_createHeaders | commons-csv | 22 | 9.09% | 45.45% | +36.36 pp | 8 | 1 |
| AbstractXMLOutputProcessor_printContent | jdom2 | 12 | 58.33% | 91.67% | +33.33 pp | 4 | 3 |
| AbstractStAXEventProcessor_printContent | jdom2 | 12 | 33.33% | 66.67% | +33.33 pp | 4 | 4 |

## 7. 21 task không hoàn tất

| Trạng thái | Số | Nguyên nhân |
| :-- | --: | :-- |
| `SEED_MISSING` | 16 | ruler — dataset chỉ có 3/16 file seed KTester |
| `SEED_UNCOMPILABLE` | 4 | seed dùng switch expression Java 14+, project đặt source level 11 |
| `SEED_STILL_RED` | 1 | jdom2 — disable hết test lỗi rồi suite vẫn đỏ |

Tất cả bị **loại khỏi aggregate**, không chấm MS = 0.

## 8. Đối chiếu với số công bố của paper KTester

| Metric | Paper | Arm K đo được | Nhận xét |
| :-- | --: | --: | :-- |
| LC | 61.10% | 60.64% | khớp — xác nhận phép đo đúng |
| BC | 52.59% | 53.27% | khớp |
| AvTC | 7.33 | 9.31 | gần |
| CPR | 100% | 100% | khớp |
| EPR | 77.07% | 15.56% | **khác định nghĩa**, không so được |
| LCP / BCP | 54.49% / 46.23% | 11.19% / 9.75% | **khác định nghĩa**, không so được |

Paper tính CPR/EPR **trên từng test case** (`passed_cases/total_cases`), còn pipeline này tính **trên từng task** (task chỉ pass khi toàn bộ test xanh). Paper tính LCP bằng cách chạy lại riêng các test pass rồi đo coverage; pipeline này gán 0 khi suite đỏ. Phần so sánh cặp K vs F vẫn hợp lệ vì hai arm dùng chung một định nghĩa.
