# KTester + MutGen fusion — toàn bộ metric của hai paper

- Run root: `/home/hactt13/rs_testing/mutgen_repo/observation_fuse/results/fuse_qwen25_coder_32b`
- Arm K = test class KTester; Arm F = chính class đó sau khi MutGen thêm test
- Stage A (seed): `artifact` | Stage B: `ollama/qwen2.5-coder:32b`
- Mọi metric scoped đúng focal method (JaCoCo theo overload, PIT theo dòng)

**90/111 focal method hoàn tất.** Mutant: 832 → 1144 / 2814 bị kill (+283 mới, −4 mất). Thắng-thua theo method: fuse 46, ktester 3, hoà 41.

## 1. Metric paper

Cột *KTester (paper)* là số công bố trong paper KTester (gpt-4o-mini, toàn dataset) — mốc tham chiếu, không phải so sánh cặp. Hai cột sau mới là so sánh cặp trên cùng workspace.

| Metric | KTester (paper) | Arm K (đo được) | Arm F (fused) | Δ F−K |
| :-- | --: | --: | --: | --: |
| CPR | 100.00% | 100.00% | 100.00% | +0.00 pp |
| EPR | 77.07% | 15.56% | 15.56% | +0.00 pp |
| LC | 61.10% | 60.64% | 61.45% | +0.80 pp |
| BC | 52.59% | 53.27% | 54.26% | +0.99 pp |
| IC | — | 59.28% | 59.97% | +0.68 pp |
| LCP | 54.49% | 11.19% | 12.31% | +1.12 pp |
| BCP | 46.23% | 9.75% | 11.31% | +1.56 pp |
| ICP | — | 11.18% | 12.36% | +1.18 pp |
| AvTC | 7.33 | 9.31 | 14.26 | +4.94 |
| AvT (s) | 152.68 | — | 88.06 | — |
| MS | — | 33.31% | 43.19% | +9.87 pp |
| Kills/test | — | 1.00 | 0.87 | -0.13 |

### Kiểm định cặp (Wilcoxon signed-rank)

- **Mutation score**: p = 5.092e-09, effect size (rank-biserial) = +0.878, 46 tăng / 3 giảm trên 49 cặp khác 0
- **Line coverage**: p = 0.2818, effect size (rank-biserial) = +0.320, 33 tăng / 17 giảm trên 50 cặp khác 0
- **Branch coverage**: p = 0.2418, effect size (rank-biserial) = +0.267, 38 tăng / 22 giảm trên 60 cặp khác 0

## 2. Theo mutation operator (RQ2)

| Mutation operator | Mutants | Arm K MS | Arm F MS | Δ |
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
| InvertNegs | 10 | 20.00% | 20.00% | +0.00 pp |

## 3. Theo từng focal method

| Focal method | Project | Mut | KT n | KT MS% | KT LC% | KT BC% | F n | F MS% | F LC% | F BC% | ΔMS | ΔLC | ΔBC | +kill | -kill | Winner |
| :-- | :-- | --: | --: | --: | --: | --: | --: | --: | --: | --: | --: | --: | --: | --: | --: | --: |
| FakeValuesService_resolveExpression | datafaker | 35 | 9 | 2.86 | 50.0 | 47.73 | 11 | 62.86 | 52.5 | 38.64 | 60.0 | 2.5 | -9.09 | 18 | 0 | fuse |
| DoubleMetaphone_doubleMetaphone | commons-codec | 48 | 14 | 35.42 | 69.7 | 60.42 | 35 | 89.58 | 80.3 | 85.42 | 54.167 | 10.61 | 25.0 | 14 | 0 | fuse |
| XPathHelper_getSingleStep | jdom2 | 22 | 3 | 0.0 | 46.15 | 36.67 | 10 | 50.0 | 66.67 | 50.0 | 50.0 | 20.51 | 13.33 | 11 | 0 | fuse |
| QuotedPrintableCodec_encodeQuotedPrintable | commons-codec | 42 | 1 | 2.38 | 17.95 | 10.0 | 6 | 42.86 | 74.36 | 62.5 | 40.476 | 56.41 | 52.5 | 17 | 0 | fuse |
| Flat3Map_remove | commons-collections | 36 | 10 | 47.22 | 45.69 | 44.23 | 20 | 86.11 | 88.79 | 69.23 | 38.889 | 43.1 | 25.0 | 12 | 0 | fuse |
| ColognePhonetic_colognePhonetic | commons-codec | 46 | 18 | 41.3 | 89.58 | 83.02 | 39 | 78.26 | 95.83 | 84.91 | 36.957 | 6.25 | 1.89 | 12 | 0 | fuse |
| MurmurHash3_hash128x64Internal | commons-codec | 92 | 7 | 0.0 | 96.49 | 27.78 | 17 | 36.96 | 100.0 | 22.22 | 36.956 | 3.51 | -5.56 | 34 | 0 | fuse |
| CSVParser_createHeaders | commons-csv | 22 | 8 | 9.09 | 16.28 | 5.26 | 9 | 45.45 | 76.74 | 44.74 | 36.364 | 60.47 | 39.47 | 8 | 0 | fuse |
| AbstractXMLOutputProcessor_printContent | jdom2 | 12 | 9 | 58.33 | 79.17 | 71.43 | 12 | 91.67 | 91.67 | 85.71 | 33.333 | 12.5 | 14.29 | 4 | 0 | fuse |
| AbstractStAXEventProcessor_printContent | jdom2 | 12 | 10 | 33.33 | 56.0 | 57.14 | 14 | 66.67 | 80.0 | 71.43 | 33.333 | 24.0 | 14.29 | 4 | 0 | fuse |
| Flat3Map_containsValue | commons-collections | 17 | 12 | 64.71 | 88.89 | 70.83 | 16 | 94.12 | 100.0 | 87.5 | 29.412 | 11.11 | 16.67 | 5 | 0 | fuse |
| IteratorUtils_getIterator | commons-collections | 24 | 6 | 41.67 | 46.43 | 50.0 | 11 | 70.83 | 67.86 | 75.0 | 29.167 | 21.43 | 25.0 | 7 | 0 | fuse |
| JsonReader_doPeek | gson | 59 | 7 | 0.0 | 34.62 | 28.79 | 13 | 28.81 | 51.28 | 46.97 | 28.814 | 16.67 | 18.18 | 17 | 0 | fuse |
| CSVFormat_toString | commons-csv | 12 | 11 | 75.0 | 97.06 | 81.82 | 14 | 100.0 | 100.0 | 86.36 | 25.0 | 2.94 | 4.55 | 3 | 0 | fuse |
| Format_escapeAttribute | jdom2 | 25 | 7 | 36.0 | 87.04 | 80.0 | 12 | 60.0 | 70.37 | 67.5 | 24.0 | -16.67 | -12.5 | 6 | 0 | fuse |
| Metaphone_metaphone | commons-codec | 91 | 14 | 41.76 | 71.07 | 63.97 | 29 | 64.84 | 82.64 | 74.26 | 23.077 | 11.57 | 10.29 | 17 | 0 | fuse |
| Rule_pattern | commons-codec | 31 | 13 | 32.26 | 100.0 | 97.5 | 26 | 54.84 | 100.0 | 97.5 | 22.581 | 0.0 | 0.0 | 6 | 0 | fuse |
| AntPathMatcher_isMatch | windward | 32 | 14 | 56.25 | 92.0 | 88.89 | 27 | 78.12 | 88.0 | 86.11 | 21.875 | -4.0 | -2.78 | 6 | 0 | fuse |
| Nysiis_nysiis | commons-codec | 34 | 16 | 58.82 | 91.67 | 88.46 | 27 | 79.41 | 100.0 | 96.15 | 20.588 | 8.33 | 7.69 | 6 | 0 | fuse |
| Flat3Map_put | commons-collections | 25 | 10 | 52.0 | 72.92 | 61.9 | 17 | 72.0 | 81.25 | 78.57 | 20.0 | 8.33 | 16.67 | 4 | 0 | fuse |
| JsonReader_nextUnquotedValue | gson | 16 | 8 | 50.0 | 63.16 | 60.0 | 11 | 68.75 | 94.74 | 80.0 | 18.75 | 31.58 | 20.0 | 3 | 0 | fuse |
| JsonReader_peek | gson | 11 | 15 | 63.64 | 66.67 | 53.85 | 18 | 81.82 | 80.0 | 69.23 | 18.182 | 13.33 | 15.38 | 2 | 0 | fuse |
| Flat3Map_equals | commons-collections | 23 | 13 | 43.48 | 82.14 | 69.23 | 19 | 60.87 | 89.29 | 76.92 | 17.391 | 7.14 | 7.69 | 4 | 0 | fuse |
| Lexer_nextToken | commons-csv | 24 | 5 | 4.17 | 43.59 | 39.47 | 6 | 20.83 | 38.46 | 31.58 | 16.667 | -5.13 | -7.89 | 4 | 0 | fuse |
| JsonReader_nextNonWhitespace | gson | 27 | 12 | 48.15 | 86.27 | 77.78 | 16 | 62.96 | 88.24 | 77.78 | 14.815 | 1.96 | 0.0 | 4 | 0 | fuse |
| CSVParser_nextRecord | commons-csv | 14 | 8 | 71.43 | 86.21 | 77.78 | 11 | 85.71 | 86.21 | 77.78 | 14.286 | 0.0 | 0.0 | 2 | 0 | fuse |
| HelpFormatter_renderOptions | commons-cli | 17 | 11 | 0.0 | 97.37 | 76.92 | 12 | 11.76 | 94.74 | 69.23 | 11.765 | -2.63 | -7.69 | 2 | 0 | fuse |
| Nysiis_transcodeRemaining | commons-codec | 26 | 17 | 69.23 | 80.0 | 75.76 | 22 | 80.77 | 95.0 | 84.85 | 11.538 | 15.0 | 9.09 | 3 | 0 | fuse |
| Verifier_isXMLPublicIDCharacter | jdom2 | 36 | 5 | 88.89 | 100.0 | 96.88 | 9 | 100.0 | 100.0 | 100.0 | 11.111 | 0.0 | 3.12 | 2 | 0 | fuse |
| Verifier_isXMLExtender | jdom2 | 38 | 6 | 76.32 | 100.0 | 93.33 | 16 | 86.84 | 100.0 | 100.0 | 10.526 | 0.0 | 6.67 | 3 | 0 | fuse |
| DoubleMetaphone_handleC | commons-codec | 40 | 10 | 37.5 | 70.59 | 62.5 | 15 | 47.5 | 73.53 | 68.75 | 10.0 | 2.94 | 6.25 | 4 | 0 | fuse |
| Flat3Map_containsKey | commons-collections | 22 | 10 | 86.36 | 95.0 | 68.75 | 22 | 95.45 | 100.0 | 84.38 | 9.091 | 5.0 | 15.62 | 2 | 0 | fuse |
| Base32_encode | commons-codec | 122 | 13 | 2.46 | 40.54 | 57.14 | 20 | 10.66 | 41.89 | 60.71 | 8.197 | 1.35 | 3.57 | 10 | 0 | fuse |
| JsonTreeReader_peek | gson | 25 | 8 | 84.0 | 90.0 | 88.46 | 9 | 92.0 | 93.33 | 92.31 | 8.0 | 3.33 | 3.85 | 2 | 0 | fuse |
| Format_escapeText | jdom2 | 25 | 9 | 40.0 | 63.27 | 68.42 | 16 | 48.0 | 73.47 | 78.95 | 8.0 | 10.2 | 10.53 | 2 | 0 | fuse |
| JsonReader_readEscapeCharacter | gson | 40 | 8 | 45.0 | 53.57 | 38.71 | 14 | 52.5 | 75.0 | 67.74 | 7.5 | 21.43 | 29.03 | 3 | 0 | fuse |
| $Gson$Types_getGenericSupertype | gson | 16 | 15 | 31.25 | 89.47 | 83.33 | 19 | 37.5 | 84.21 | 72.22 | 6.25 | -5.26 | -11.11 | 1 | 0 | fuse |
| LinkedTreeMap_find | gson | 18 | 11 | 44.44 | 90.91 | 81.82 | 18 | 50.0 | 96.97 | 81.82 | 5.556 | 6.06 | 0.0 | 1 | 0 | fuse |
| Sha2Crypt_sha2Crypt | commons-codec | 102 | 7 | 3.92 | 93.16 | 79.41 | 40 | 8.82 | 96.58 | 85.29 | 4.902 | 3.42 | 5.88 | 5 | 0 | fuse |
| Base64_decode | commons-codec | 41 | 9 | 19.51 | 80.56 | 83.33 | 26 | 24.39 | 80.56 | 87.5 | 4.878 | 0.0 | 4.17 | 2 | 0 | fuse |
| ReflectiveTypeAdapterFactory_getBoundFields | gson | 25 | 1 | 0.0 | 28.26 | 20.0 | 2 | 4.0 | 6.52 | 2.5 | 4.0 | -21.74 | -17.5 | 1 | 0 | fuse |
| WalkerNORMALIZE_analyzeMultiText | jdom2 | 25 | 4 | 0.0 | 15.15 | 3.03 | 5 | 4.0 | 15.15 | 3.03 | 4.0 | 0.0 | 0.0 | 1 | 0 | fuse |
| CSVFormat_printWithQuotes | commons-csv | 26 | 10 | 57.69 | 79.63 | 67.44 | 20 | 61.54 | 81.48 | 72.09 | 3.846 | 1.85 | 4.65 | 1 | 0 | fuse |
| Base32_decode | commons-codec | 85 | 11 | 38.82 | 50.0 | 67.86 | 19 | 42.35 | 55.17 | 75.0 | 3.529 | 5.17 | 7.14 | 3 | 0 | fuse |
| Base64_encode | commons-codec | 62 | 11 | 32.26 | 40.91 | 33.33 | 18 | 35.48 | 45.45 | 40.0 | 3.226 | 4.55 | 6.67 | 2 | 0 | fuse |
| SequencesComparator_getMiddleSnake | commons-collections | 95 | 6 | 44.21 | 97.22 | 82.69 | 15 | 47.37 | 97.22 | 84.62 | 3.158 | 0.0 | 1.92 | 2 | 0 | fuse |
| SparkPodSpec_copyFrom | batch-processing-gateway | 9 | 11 | 0.0 | 0.0 | 0.0 | 18 | 0.0 | 0.0 | 0.0 | 0 | 0.0 | 0.0 | 0 | 0 | tie |
| ApplicationSubmissionHelper_getSparkConf | batch-processing-gateway | 12 | 12 | 66.67 | 93.55 | 88.46 | 16 | 66.67 | 87.1 | 76.92 | 0.0 | -6.45 | -11.54 | 0 | 0 | tie |
| SubmissionSummary_copyFrom | batch-processing-gateway | 34 | 8 | 76.47 | 98.51 | 70.0 | 8 | 76.47 | 94.03 | 56.67 | 0.0 | -4.48 | -13.33 | 0 | 0 | tie |
| ApplicationSubmissionHelper_getDriverSpec | batch-processing-gateway | 37 | 10 | 0.0 | 33.9 | 41.67 | 10 | 0.0 | 0.0 | 0.0 | 0 | -33.9 | -41.67 | 0 | 0 | tie |
| ApplicationSubmissionHelper_populateEnv | batch-processing-gateway | 28 | 9 | 25.0 | 38.46 | 43.75 | 13 | 25.0 | 38.46 | 43.75 | 0.0 | 0.0 | 0.0 | 0 | 0 | tie |
| ApplicationSubmissionHelper_getExecutorSpec | batch-processing-gateway | 37 | 11 | 27.03 | 66.15 | 50.0 | 14 | 27.03 | 66.15 | 47.06 | 0.0 | 0.0 | -2.94 | 0 | 0 | tie |
| DoubleMetaphone_handleG | commons-codec | 59 | 15 | 10.17 | 35.0 | 24.0 | 20 | 10.17 | 35.0 | 22.0 | 0.0 | 0.0 | -2.0 | 0 | 0 | tie |
| DaitchMokotoffSoundex_soundex | commons-codec | 27 | 16 | 70.37 | 97.73 | 90.48 | 19 | 70.37 | 97.73 | 90.48 | 0.0 | 0.0 | 0.0 | 0 | 0 | tie |
| Flat3Map_get | commons-collections | 20 | 10 | 95.0 | 100.0 | 75.0 | 32 | 95.0 | 100.0 | 87.5 | 0.0 | 0.0 | 12.5 | 0 | 0 | tie |
| AbstractPatriciaTrie_put | commons-collections | 22 | 13 | 0.0 | 24.14 | 15.0 | 14 | 0.0 | 24.14 | 15.0 | 0 | 0.0 | 0.0 | 0 | 0 | tie |
| AbstractPatriciaTrie_nextEntryImpl | commons-collections | 21 | 8 | 0.0 | 46.67 | 43.75 | 18 | 0.0 | 46.67 | 46.88 | 0 | 0.0 | 3.12 | 0 | 0 | tie |
| TreeBidiMap_doRedBlackDelete | commons-collections | 24 | 7 | 4.17 | 60.71 | 50.0 | 10 | 4.17 | 32.14 | 25.0 | 0.0 | -28.57 | -25.0 | 0 | 0 | tie |
| TreeBidiMap_swapPosition | commons-collections | 43 | 8 | 0.0 | 53.85 | 33.33 | 8 | 0.0 | 53.85 | 33.33 | 0 | 0.0 | 0.0 | 0 | 0 | tie |
| AbstractPatriciaTrie_higherEntry | commons-collections | 20 | 13 | 0.0 | 21.43 | 22.22 | 16 | 0.0 | 21.43 | 22.22 | 0 | 0.0 | 0.0 | 0 | 0 | tie |
| Lexer_parseEncapsulatedToken | commons-csv | 13 | 11 | 0.0 | 38.71 | 35.0 | 11 | 0.0 | 0.0 | 0.0 | 0 | -38.71 | -35.0 | 0 | 0 | tie |
| SqlTransformer_handlePrimitivesInArray | datafaker | 22 | 14 | 0.0 | 0.0 | 0.0 | 14 | 0.0 | 0.0 | 0.0 | 0 | 0.0 | 0.0 | 0 | 0 | tie |
| FakeValuesService_bothify | datafaker | 8 | 11 | 0.0 | 81.82 | 78.57 | 11 | 0.0 | 0.0 | 0.0 | 0 | -81.82 | -78.57 | 0 | 0 | tie |
| ConstructorConstructor_newDefaultImplementationConstructor | gson | 19 | 10 | 63.16 | 80.0 | 75.0 | 11 | 63.16 | 80.0 | 75.0 | 0.0 | 0.0 | 0.0 | 0 | 0 | tie |
| $Gson$Types_resolve | gson | 25 | 0 | 0.0 | 0.0 | 0.0 | 0 | 0.0 | 0.0 | 0.0 | 0 | 0.0 | 0.0 | 0 | 0 | tie |
| JsonReader_skipValue | gson | 28 | 10 | 28.57 | 63.41 | 48.0 | 11 | 28.57 | 41.46 | 28.0 | 0.0 | -21.95 | -20.0 | 0 | 0 | tie |
| Primitives_unwrap | gson | 19 | 13 | 100.0 | 100.0 | 100.0 | 13 | 100.0 | 100.0 | 100.0 | 0.0 | 0.0 | 0.0 | 0 | 0 | tie |
| JsonReader_peekNumber | gson | 50 | 8 | 0.0 | 47.62 | 31.88 | 8 | 0.0 | 0.0 | 0.0 | 0 | -47.62 | -31.88 | 0 | 0 | tie |
| Excluder_excludeField | gson | 24 | 6 | 16.67 | 43.48 | 28.12 | 6 | 16.67 | 43.48 | 28.12 | 0.0 | 0.0 | 0.0 | 0 | 0 | tie |
| TypeToken_isAssignableFrom | gson | 15 | 9 | 33.33 | 92.31 | 77.78 | 15 | 33.33 | 92.31 | 88.89 | 0.0 | 0.0 | 11.11 | 0 | 0 | tie |
| Primitives_wrap | gson | 19 | 14 | 100.0 | 100.0 | 100.0 | 14 | 100.0 | 100.0 | 100.0 | 0.0 | 0.0 | 0.0 | 0 | 0 | tie |
| LinkedTreeMap_rebalance | gson | 31 | 2 | 0.0 | 0.0 | 0.0 | 2 | 0.0 | 0.0 | 0.0 | 0 | 0.0 | 0.0 | 0 | 0 | tie |
| ObjectTypeAdapter_read | gson | 14 | 12 | 78.57 | 96.67 | 94.44 | 12 | 78.57 | 96.67 | 94.44 | 0.0 | 0.0 | 0.0 | 0 | 0 | tie |
| $Gson$Types_equals | gson | 30 | 13 | 66.67 | 87.88 | 76.47 | 14 | 66.67 | 87.88 | 76.47 | 0.0 | 0.0 | 0.0 | 0 | 0 | tie |
| AbstractStAXStreamProcessor_printDocument | jdom2 | 32 | 0 | 0.0 | 0.0 | 0.0 | 0 | 0.0 | 0.0 | 0.0 | 0 | 0.0 | 0.0 | 0 | 0 | tie |
| StringBin_locate | jdom2 | 48 | 8 | 33.33 | 46.43 | 26.92 | 15 | 33.33 | 46.43 | 26.92 | 0.0 | 0.0 | 0.0 | 0 | 0 | tie |
| DTDParser_formatInternal | jdom2 | 12 | 12 | 0.0 | 0.0 | 0.0 | 12 | 0.0 | 0.0 | 0.0 | 0 | 0.0 | 0.0 | 0 | 0 | tie |
| AbstractStAXStreamProcessor_printContent | jdom2 | 12 | 9 | 0.0 | 0.0 | 0.0 | 9 | 0.0 | 0.0 | 0.0 | 0 | 0.0 | 0.0 | 0 | 0 | tie |
| AbstractSAXOutputProcessor_printContent | jdom2 | 11 | 7 | 0.0 | 8.7 | 14.29 | 7 | 0.0 | 8.7 | 7.14 | 0 | 0.0 | -7.14 | 0 | 0 | tie |
| AbstractFormattedWalker_next | jdom2 | 36 | 3 | 5.56 | 24.44 | 22.22 | 3 | 5.56 | 24.44 | 22.22 | 0.0 | 0.0 | 0.0 | 0 | 0 | tie |
| StAXStreamBuilder_processPrunableElement | jdom2 | 21 | 2 | 4.76 | 4.65 | 2.78 | 2 | 4.76 | 4.65 | 2.78 | 0.0 | 0.0 | 0.0 | 0 | 0 | tie |
| Element_getNamespace | jdom2 | 15 | 9 | 93.33 | 94.74 | 80.0 | 10 | 93.33 | 94.74 | 85.0 | 0.0 | 0.0 | 5.0 | 0 | 0 | tie |
| Element_getNamespacesInScope | jdom2 | 9 | 11 | 77.78 | 100.0 | 86.36 | 12 | 77.78 | 85.19 | 68.18 | 0.0 | -14.81 | -18.18 | 0 | 0 | tie |
| AbstractStAXEventProcessor_printDocument | jdom2 | 27 | 11 | 0.0 | 23.08 | 18.18 | 11 | 0.0 | 0.0 | 0.0 | 0 | -23.08 | -18.18 | 0 | 0 | tie |
| WalkerTRIM_analyzeMultiText | jdom2 | 23 | 5 | 0.0 | 0.0 | 0.0 | 5 | 0.0 | 0.0 | 0.0 | 0 | 0.0 | 0.0 | 0 | 0 | tie |
| SAXHandler_startElement | jdom2 | 39 | 2 | 0.0 | 25.37 | 15.38 | 2 | 0.0 | 0.0 | 0.0 | 0 | -25.37 | -15.38 | 0 | 0 | tie |
| AbstractRouterGroup_matchRouter | windward | 20 | 3 | 0.0 | 0.0 | 0.0 | 7 | 0.0 | 0.0 | 0.0 | 0 | 0.0 | 0.0 | 0 | 0 | tie |
| FakeValues_toJavaNames | datafaker | 30 | 13 | 63.33 | 100.0 | 84.38 | 19 | 60.0 | 100.0 | 90.62 | -3.333 | 0.0 | 6.25 | 1 | 1 | ktester |
| FakeValuesService_javaNameToYamlName | datafaker | 28 | 11 | 78.57 | 100.0 | 100.0 | 17 | 75.0 | 100.0 | 100.0 | -3.571 | 0.0 | 0.0 | 0 | 1 | ktester |
| FakeValuesService_resolveExpression_2 | datafaker | 23 | 6 | 8.7 | 96.15 | 87.5 | 11 | 0.0 | 96.15 | 87.5 | -8.696 | 0.0 | 0.0 | 0 | 2 | ktester |
