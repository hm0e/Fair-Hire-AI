# Rule: Empirical Performance Auditing & Algorithmic Complexity

1. **Empirical Scaling Verification:**
   - When reporting benchmark results across variable input sizes ($N_1 \to N_2$), calculate the growth ratio $\frac{T(N_2)}{T(N_1)}$.
   - If $\frac{T(N_2)}{T(N_1)} \gg \frac{N_2}{N_1}$, do NOT characterize the scaling as linear. Flag the superlinear or polynomial growth and profile the execution hot path.

2. **Non-Overlapping Text Span Reservation:**
   - In string tokenization, NER, and entity extraction where longer spans take priority and prevent substring collisions, avoid $O(M^2)$ pairwise interval comparisons across dynamically appended lists.
   - Use $O(1)$ bit-level indexing (`BitSet`) or $O(\log M)$ interval indices (`TreeMap`) to prevent quadratic ReDoS / algorithmic degradation on large documents.

3. **Strict Phase Gate Discipline:**
   - When assigned to evaluate or validate a specific project phase gate, under no circumstance introduce or touch features from subsequent phases.
   - Every defect or performance anomaly must be resolved or formally classified with measured evidence before granting phase approval.
