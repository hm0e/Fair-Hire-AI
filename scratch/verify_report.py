with open('docs/qa/phase-4e-production-readiness-report.md', 'r', encoding='utf-8') as f:
    lines = f.readlines()

print('=== 1. Literal search for "mvnw\\.cmd" ===')
matches_mvnw_esc = [(i + 1, l.rstrip()) for i, l in enumerate(lines) if r'mvnw\.cmd' in l]
print(f'Matches count: {len(matches_mvnw_esc)}')
for num, l in matches_mvnw_esc:
    print(f'Line {num}: {l}')

print('\n=== 2. Literal search for ".\\mvnw\\.cmd" ===')
matches_dot_mvnw_esc = [(i + 1, l.rstrip()) for i, l in enumerate(lines) if r'.\mvnw\.cmd' in l]
print(f'Matches count: {len(matches_dot_mvnw_esc)}')
for num, l in matches_dot_mvnw_esc:
    print(f'Line {num}: {l}')

print('\n=== 3. Exact matching lines containing "mvnw" ===')
matches_mvnw = [(i + 1, l.rstrip()) for i, l in enumerate(lines) if 'mvnw' in l]
print(f'Matches count: {len(matches_mvnw)}')
for num, l in matches_mvnw:
    print(f'Line {num}: {l}')

print('\n=== 4. Section 25 Full Regression Summary Excerpt ===')
for i in range(408, 430):
    if i < len(lines):
        print(f'{i+1}: {lines[i].rstrip()}')

print('\n=== 5. Section 36 Regression Summary Excerpt ===')
for i in range(704, 726):
    if i < len(lines):
        print(f'{i+1}: {lines[i].rstrip()}')
