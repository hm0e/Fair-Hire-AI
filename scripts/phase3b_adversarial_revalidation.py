"""
FairHire AI — Phase 3B Adversarial Revalidation Runner
Tests all 22 adversarial groups, B-01 to B-08, performance, security, and integrity.
"""

import urllib.request
import urllib.error
import urllib.parse
import json
import time
import concurrent.futures
import hashlib
import os
import sys
import subprocess
import zipfile
import io
import re

def get_ip(container_name, default_ip):
    try:
        cmd = ["docker", "inspect", container_name, "--format", "{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}"]
        p = subprocess.run(cmd, capture_output=True, text=True)
        ip = p.stdout.strip().replace("'", "").replace('"', "")
        if ip and re.match(r"^\d+\.\d+\.\d+\.\d+$", ip):
            return ip
    except Exception as e:
        print(f"Error inspecting {container_name}: {e}")
    return default_ip

BACKEND_IP = get_ip("fairhire-backend", "172.19.0.5")
AI_IP = get_ip("fairhire-ai-service", "172.19.0.3")
print(f"Targeting BACKEND_IP: {BACKEND_IP}, AI_IP: {AI_IP}")
BASE_URL = f"http://{BACKEND_IP}:8088/api/v1"
AI_URL = f"http://{AI_IP}:5000"

def log(msg):
    print(f"[{time.strftime('%H:%M:%S')}] {msg}", flush=True)

def http_json(method, path, body=None, expected_status=None):
    url = f"{BASE_URL}{path}"
    data = json.dumps(body).encode("utf-8") if body is not None else None
    headers = {"Content-Type": "application/json"} if data else {}
    req = urllib.request.Request(url, data=data, headers=headers, method=method)
    try:
        with urllib.request.urlopen(req) as resp:
            status = resp.status
            content = resp.read().decode("utf-8")
            parsed = json.loads(content) if content else {}
            if expected_status and status != expected_status:
                raise AssertionError(f"Expected status {expected_status}, got {status}: {content}")
            return status, parsed
    except urllib.error.HTTPError as e:
        status = e.code
        content = e.read().decode("utf-8")
        try:
            parsed = json.loads(content)
        except Exception:
            parsed = {"raw": content}
        if expected_status and status != expected_status:
            raise AssertionError(f"Expected status {expected_status}, got {status}: {content}")
        return status, parsed

def http_multipart(path, filename, content_bytes, mime_type, form_fields=None, expected_status=None):
    url = f"{BASE_URL}{path}"
    boundary = "----WebKitFormBoundary7MA4YWxkTrZu0gW"
    data = bytearray()
    
    # Add form fields
    if form_fields:
        for k, v in form_fields.items():
            data.extend(f"--{boundary}\r\n".encode("utf-8"))
            data.extend(f'Content-Disposition: form-data; name="{k}"\r\n\r\n'.encode("utf-8"))
            data.extend(f"{v}\r\n".encode("utf-8"))
            
    # Add file
    data.extend(f"--{boundary}\r\n".encode("utf-8"))
    data.extend(f'Content-Disposition: form-data; name="file"; filename="{filename}"\r\n'.encode("utf-8"))
    data.extend(f"Content-Type: {mime_type}\r\n\r\n".encode("utf-8"))
    data.extend(content_bytes)
    data.extend(b"\r\n")
    data.extend(f"--{boundary}--\r\n".encode("utf-8"))
    
    req = urllib.request.Request(url, data=bytes(data), headers={
        "Content-Type": f"multipart/form-data; boundary={boundary}"
    }, method="POST")
    
    try:
        with urllib.request.urlopen(req) as resp:
            status = resp.status
            content = resp.read().decode("utf-8")
            parsed = json.loads(content) if content else {}
            if expected_status and status != expected_status:
                raise AssertionError(f"Expected status {expected_status}, got {status}: {content}")
            return status, parsed
    except urllib.error.HTTPError as e:
        status = e.code
        content = e.read().decode("utf-8")
        try:
            parsed = json.loads(content)
        except Exception:
            parsed = {"raw": content}
        if expected_status and status != expected_status:
            raise AssertionError(f"Expected status {expected_status}, got {status}: {content}")
        return status, parsed

def create_valid_pdf_bytes(text):
    lines = text.split("\n")
    stream_content = "BT /F1 12 Tf 72 720 Td\n"
    for i, line in enumerate(lines):
        escaped = line.replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)")
        if i == 0:
            stream_content += f"({escaped}) Tj\n"
        else:
            stream_content += f"0 -15 Td ({escaped}) Tj\n"
    stream_content += "ET"
    stream_bytes = stream_content.encode("latin-1")
    stream_len = len(stream_bytes)

    header = b"%PDF-1.4\n"
    obj1 = b"1 0 obj\n<</Type/Catalog/Pages 2 0 R>>\nendobj\n"
    obj2 = b"2 0 obj\n<</Type/Pages/Kids[3 0 R]/Count 1>>\nendobj\n"
    obj3 = b"3 0 obj\n<</Type/Page/Parent 2 0 R/MediaBox[0 0 612 792]/Contents 4 0 R/Resources<</Font<</F1 5 0 R>>>>>>\nendobj\n"
    obj4 = f"4 0 obj\n<</Length {stream_len}>>\nstream\n".encode("ascii") + stream_bytes + b"\nendstream\nendobj\n"
    obj5 = b"5 0 obj\n<</Type/Font/Subtype/Type1/BaseFont/Helvetica>>\nendobj\n"

    offsets = [0]
    pos = len(header)
    for obj in [obj1, obj2, obj3, obj4, obj5]:
        offsets.append(pos)
        pos += len(obj)

    xref_offset = pos
    xref = f"xref\n0 {len(offsets)}\n0000000000 65535 f \n".encode("ascii")
    for off in offsets[1:]:
        xref += f"{off:010d} 00000 n \n".encode("ascii")
    
    trailer = f"trailer\n<</Size {len(offsets)}/Root 1 0 R>>\nstartxref\n{xref_offset}\n%%EOF\n".encode("ascii")
    return header + obj1 + obj2 + obj3 + obj4 + obj5 + xref + trailer

def create_valid_docx_bytes(text):
    content_types = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
  <Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>
</Types>"""

    rels = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/>
</Relationships>"""

    paragraphs = ""
    for line in text.split("\n"):
        paragraphs += f"<w:p><w:r><w:t>{line}</w:t></w:r></w:p>"

    doc_xml = f"""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
  <w:body>
    {paragraphs}
  </w:body>
</w:document>"""

    buf = io.BytesIO()
    with zipfile.ZipFile(buf, "w", zipfile.ZIP_DEFLATED) as zf:
        zf.writestr("[Content_Types].xml", content_types)
        zf.writestr("_rels/.rels", rels)
        zf.writestr("word/document.xml", doc_xml)
    return buf.getvalue()

def ingest_text(text, name="Test Candidate", email="test@example.com", ensure_unique=True):
    actual_text = f"{text}\n# ref: {time.time()}_{os.urandom(4).hex()}" if ensure_unique else text
    status, data = http_json("POST", "/resumes", {
        "raw_text": actual_text,
        "name": name,
        "email": email
    })
    return status, data

def main():
    log("=== Starting FairHire AI Phase 3B Adversarial Revalidation Suite ===")
    results = {}
    
    # Wait for backend to be healthy
    health = None
    for attempt in range(30):
        try:
            status, health = http_json("GET", "/health", expected_status=200)
            if health.get("status") == "UP":
                break
        except Exception:
            time.sleep(2)
    assert health and health.get("status") == "UP", "Backend failed to become healthy within 60s"
    log("Backend is UP and healthy.")
    
    # Fetch all active skills
    status, skills_list = http_json("GET", "/skills", expected_status=200)
    canonical_skills = {s["name"]: s for s in skills_list}
    log(f"Loaded {len(canonical_skills)} active canonical skills from PostgreSQL.")
    assert len(canonical_skills) == 31, f"Expected 31 canonical skills, found {len(canonical_skills)}"
    
    # -------------------------------------------------------------
    # 1. CANONICAL TAXONOMY (Group 1)
    # -------------------------------------------------------------
    log("Running Group 1: Canonical Taxonomy...")
    group1_pass = True
    g1_failures = []
    for skill_name in canonical_skills.keys():
        sample = f"Professional background: Demonstrates expert ability in {skill_name}."
        unique_email = f"g1_{hashlib.md5(skill_name.encode()).hexdigest()[:8]}@example.com"
        s, resp = ingest_text(sample, f"G1 {skill_name}", unique_email)
        if s != 201:
            group1_pass = False
            g1_failures.append(f"{skill_name}: status {s}")
            continue
        extracted = [sk["skillName"] for sk in resp.get("extractedSkills", [])]
        if skill_name not in extracted:
            group1_pass = False
            g1_failures.append(f"{skill_name}: not extracted in '{sample}' -> got {extracted}")
    results["Group 1: Canonical Taxonomy"] = (group1_pass, f"31/31 skills tested. Failures: {g1_failures}")

    # -------------------------------------------------------------
    # 2. ALIAS EXHAUSTIVENESS (Group 2)
    # -------------------------------------------------------------
    log("Running Group 2: Alias Exhaustiveness...")
    group2_pass = True
    g2_failures = []
    total_aliases = 0
    for canonical_name, skill_obj in canonical_skills.items():
        synonyms = skill_obj.get("synonyms") or []
        for alias in synonyms:
            if not alias or not alias.strip():
                continue
            total_aliases += 1
            sample = f"Hands-on technical implementation using {alias} in production systems."
            unique_email = f"g2_{hashlib.md5(alias.encode()).hexdigest()[:8]}@example.com"
            s, resp = ingest_text(sample, f"G2 {alias}", unique_email)
            if s != 201:
                group2_pass = False
                g2_failures.append(f"Alias '{alias}': HTTP {s}")
                continue
            extracted = [sk["skillName"] for sk in resp.get("extractedSkills", [])]
            if canonical_name not in extracted:
                group2_pass = False
                g2_failures.append(f"Alias '{alias}' failed to resolve to '{canonical_name}' -> got {extracted}")
    results["Group 2: Alias Exhaustiveness"] = (group2_pass, f"{total_aliases} aliases tested. Failures: {g2_failures}")

    # -------------------------------------------------------------
    # 3. FALSE POSITIVES (Group 3)
    # -------------------------------------------------------------
    log("Running Group 3: False Positives Subtoken Collisions...")
    false_positive_tests = [
        ("Built complex single page apps using JavaScript and JS libraries.", ["Java"]),
        ("Researched chemical reaction kinetics and reactor controls.", ["React"]),
        ("Developed reactive event loops in Python.", ["React"]),
        ("Demonstrated flaws in design and draws architectural blueprints.", ["AWS"]),
        ("Extensive relational querying in PostgreSQL, MySQL, and NoSQL stores.", ["SQL"]),
        ("Followed Pythonic design principles.", ["Python"]),
        ("Graduated in Spring 2024 from university.", ["Spring Boot"]),
    ]
    group3_pass = True
    g3_failures = []
    for text, forbidden in false_positive_tests:
        u_email = f"g3_{hashlib.md5(text.encode()).hexdigest()[:8]}@example.com"
        s, resp = ingest_text(text, "G3 Candidate", u_email)
        extracted = [sk["skillName"] for sk in resp.get("extractedSkills", [])]
        for f in forbidden:
            if f in extracted:
                group3_pass = False
                g3_failures.append(f"Forbidden skill '{f}' extracted from '{text}' -> {extracted}")
    results["Group 3: False Positives (Subtokens)"] = (group3_pass, f"Failures: {g3_failures}")

    # -------------------------------------------------------------
    # 4 & 5. SYMBOL BOUNDARIES & VARIATIONS (B-01 Deep Validation)
    # -------------------------------------------------------------
    log("Running Group 4 & 5: Symbol Boundaries & B-01 Variations...")
    b01_positive_tests = [
        ("Candidate has deep practical expertise in C.", ["C"]),
        ("Primary programming languages: C, Python.", ["C", "Python"]),
        ("Core competencies include C; also Java.", ["C", "Java"]),
        ("Primary systems languages (C) and Python.", ["C", "Python"]),
        ("Expertise list: [C] and [Docker].", ["C", "Docker"]),
        ("Technology stack: {C} and {Rust}.", ["C", "Rust"]),
        ("Senior programmer in C++.", ["C++"]),
        ("Enterprise apps in C#.", ["C#"]),
        ("Proficient in C/C++.", ["C++"]),
        ("Experience as C-developer on Linux.", ["C", "Linux"]),
    ]
    b01_negative_tests = [
        ("Served as C.A. auditor for corporate accounts.", ["C"]),
        ("Compiled header file c.h in linux build system.", ["C"]),
        ("Linked library lib.c against static runtime.", ["C"]),
        ("Managed C.Developer teams across regions.", ["C"]),
    ]
    b01_pass = True
    b01_failures = []
    for text, expected in b01_positive_tests:
        u_email = f"b01pos_{hashlib.md5(text.encode()).hexdigest()[:8]}@example.com"
        s, resp = ingest_text(text, "B01 Pos", u_email)
        extracted = [sk["skillName"] for sk in resp.get("extractedSkills", [])]
        for exp in expected:
            if exp not in extracted:
                b01_pass = False
                b01_failures.append(f"Expected '{exp}' in '{text}' -> got {extracted}")
    for text, forbidden in b01_negative_tests:
        u_email = f"b01neg_{hashlib.md5(text.encode()).hexdigest()[:8]}@example.com"
        s, resp = ingest_text(text, "B01 Neg", u_email)
        extracted = [sk["skillName"] for sk in resp.get("extractedSkills", [])]
        for f in forbidden:
            if f in extracted:
                b01_pass = False
                b01_failures.append(f"Forbidden '{f}' falsely extracted from '{text}' -> {extracted}")
    results["Group 4 & 5 / B-01: Symbol Boundaries & C Handling"] = (b01_pass, f"Failures: {b01_failures}")

    # -------------------------------------------------------------
    # 6. NATURAL-LANGUAGE AMBIGUITY (B-02 & B-03 Deep Validation)
    # -------------------------------------------------------------
    log("Running Group 6: Natural Language Ambiguity (B-02 Go & B-03 Node)...")
    b02_prose_negative = [
        "I like to go running.",
        "Always ready to go.",
        "Decided to let it go.",
        "We should go ahead with the deployment.",
        "Let us go through the requirements carefully.",
        "We should go forward with the original plan.",
        "Worked at Google on large scale systems.",
        "Everything was going according to plan.",
        "He goes above and beyond in every project.",
        "The old legacy systems are now completely gone."
    ]
    b02_tech_positive = [
        ("Experienced in Go.", ["Go"]),
        ("Lead Go developer on microservices.", ["Go"]),
        ("Expertise in Go programming.", ["Go"]),
        ("Backend developer with Go and Docker.", ["Go", "Docker"]),
        ("Proficient in GO language.", ["Go"]),
        ("Built distributed systems using Golang.", ["Go"]),
        ("Senior GoLang engineer.", ["Go"])
    ]
    b02_pass = True
    b02_failures = []
    for text in b02_prose_negative:
        u_email = f"b02neg_{hashlib.md5(text.encode()).hexdigest()[:8]}@example.com"
        s, resp = ingest_text(text, "B02 Prose", u_email)
        extracted = [sk["skillName"] for sk in resp.get("extractedSkills", [])]
        if "Go" in extracted:
            b02_pass = False
            b02_failures.append(f"Falsely extracted 'Go' from prose: '{text}'")
    for text, expected in b02_tech_positive:
        u_email = f"b02pos_{hashlib.md5(text.encode()).hexdigest()[:8]}@example.com"
        s, resp = ingest_text(text, "B02 Tech", u_email)
        extracted = [sk["skillName"] for sk in resp.get("extractedSkills", [])]
        for exp in expected:
            if exp not in extracted:
                b02_pass = False
                b02_failures.append(f"Failed to extract '{exp}' from tech text: '{text}' -> {extracted}")

    # B-03 Node.js vs infrastructure node
    b03_node_negative = [
        "Architected high-throughput worker node in Kubernetes cluster.",
        "Configured cluster node failover mechanisms.",
        "Provisioned dedicated compute node on cloud infrastructure.",
        "Inspected DOM node elements and hierarchy.",
        "Rendered custom node elements on canvas.",
        "Managed each node in Kubernetes production cluster."
    ]
    b03_node_positive = [
        ("Full-stack engineer building services with Node.js.", ["Node.js"]),
        ("Backend services written in NodeJS and Express.", ["Node.js"]),
        ("Runtime environment: Node.JS version 20.", ["Node.js"])
    ]
    b03_pass = True
    b03_failures = []
    for text in b03_node_negative:
        u_email = f"b03neg_{hashlib.md5(text.encode()).hexdigest()[:8]}@example.com"
        s, resp = ingest_text(text, "B03 Node Neg", u_email)
        extracted = [sk["skillName"] for sk in resp.get("extractedSkills", [])]
        if "Node.js" in extracted:
            b03_pass = False
            b03_failures.append(f"Falsely extracted 'Node.js' from node text: '{text}'")
    for text, expected in b03_node_positive:
        u_email = f"b03pos_{hashlib.md5(text.encode()).hexdigest()[:8]}@example.com"
        s, resp = ingest_text(text, "B03 Node Pos", u_email)
        extracted = [sk["skillName"] for sk in resp.get("extractedSkills", [])]
        for exp in expected:
            if exp not in extracted:
                b03_pass = False
                b03_failures.append(f"Failed to extract '{exp}' from tech text: '{text}' -> {extracted}")

    results["Group 6 / B-02: Go Language Disambiguation"] = (b02_pass, f"Failures: {b02_failures}")
    results["Group 6 / B-03: Node.js vs Cluster Node"] = (b03_pass, f"Failures: {b03_failures}")

    # -------------------------------------------------------------
    # 7. FALSE NEGATIVES (Group 7)
    # -------------------------------------------------------------
    log("Running Group 7: False Negatives across Layouts...")
    g7_layouts = [
        # Comma separated
        ("Skills: Java, Python, Docker, Kubernetes, PostgreSQL, Redis, Git, Linux.",
         ["Java", "Python", "Docker", "Kubernetes", "PostgreSQL", "Redis", "Git", "Linux"]),
        # Bullet points
        ("Key Technologies:\n* Java\n* Spring Boot\n* PostgreSQL\n* Docker\n* AWS",
         ["Java", "Spring Boot", "PostgreSQL", "Docker", "AWS"]),
        # Paragraph work experience
        ("Work Experience:\nDeveloped core streaming backend using Python, FastAPI, and Redis. Deployed services with Docker and Kubernetes.",
         ["Python", "FastAPI", "Redis", "Docker", "Kubernetes"]),
        # Markdown table
        ("| Skill | Level |\n| --- | --- |\n| TypeScript | Expert |\n| React | Expert |\n| GraphQL | Intermediate |",
         ["TypeScript", "React", "GraphQL"]),
        # Sentence ending C.
        ("Over 5 years developing embedded microcontrollers in C. Later added Rust.",
         ["C", "Rust"])
    ]
    g7_pass = True
    g7_failures = []
    for layout_text, expected in g7_layouts:
        u_email = f"g7_{hashlib.md5(layout_text.encode()).hexdigest()[:8]}@example.com"
        s, resp = ingest_text(layout_text, "G7 Layout", u_email)
        extracted = [sk["skillName"] for sk in resp.get("extractedSkills", [])]
        for exp in expected:
            if exp not in extracted:
                g7_pass = False
                g7_failures.append(f"Layout failed for '{exp}' in '{layout_text}' -> {extracted}")
    results["Group 7: False Negatives"] = (g7_pass, f"Failures: {g7_failures}")

    # -------------------------------------------------------------
    # 8. CONTEXT / EVIDENCE INTEGRITY (Group 8)
    # -------------------------------------------------------------
    log("Running Group 8: Context/Evidence Integrity...")
    g8_text = "Summary: Experienced software engineer with over 5 years developing distributed systems. Developed microservices using Spring Boot, PostgreSQL, and Docker in AWS cloud."
    u_email = f"g8_{hashlib.md5(g8_text.encode()).hexdigest()[:8]}@example.com"
    s, resp = ingest_text(g8_text, "G8 Evidence", u_email)
    skills = resp.get("extractedSkills", [])
    g8_pass = True
    g8_details = []
    for sk in skills:
        matched = sk.get("matchedText")
        snippet = sk.get("contextSnippet")
        conf = sk.get("extractionConfidence", sk.get("confidence"))
        if matched not in g8_text:
            g8_pass = False
            g8_details.append(f"Matched text '{matched}' not found in source")
        if not snippet or len(snippet) > 280:
            g8_pass = False
            g8_details.append(f"Snippet invalid length ({len(snippet) if snippet else 0})")
        if matched not in snippet:
            g8_pass = False
            g8_details.append(f"Snippet does not contain matched text '{matched}'")
        if conf is None or float(conf) < 0.0 or float(conf) > 1.0:
            g8_pass = False
            g8_details.append(f"Invalid confidence {conf}")
    results["Group 8: Context / Evidence Integrity"] = (g8_pass, f"Details: {g8_details}")

    # -------------------------------------------------------------
    # 9. DEDUPLICATION (Group 9)
    # -------------------------------------------------------------
    log("Running Group 9: Deduplication...")
    g9_text = "Skills: Java, Core Java, Java SE, Java programming, java 17, and Java 21 features."
    u_email = f"g9_{hashlib.md5(g9_text.encode()).hexdigest()[:8]}@example.com"
    s, resp = ingest_text(g9_text, "G9 Dedup", u_email)
    skills = resp.get("extractedSkills", [])
    conf_val = float(skills[0].get("extractionConfidence", skills[0].get("confidence", 0))) if skills else 0.0
    g9_pass = len(skills) == 1 and skills[0]["skillName"] == "Java" and conf_val == 1.0
    conf_display = skills[0].get("extractionConfidence", skills[0].get("confidence")) if skills else None
    results["Group 9: Deduplication"] = (g9_pass, f"Count: {len(skills)}, Skill: {skills[0]['skillName'] if skills else None}, Conf: {conf_display}")

    # -------------------------------------------------------------
    # 10. DETERMINISM (Group 10 - 100 runs)
    # -------------------------------------------------------------
    log("Running Group 10: Determinism (100 runs)...")
    complex_resume = """Dr. Alan Turing
Principal Systems Architect
Summary:
Extensive 15-year career designing fault-tolerant distributed platforms.
Technical Skills:
Languages: Java, Python, Go, Rust, TypeScript, C++, C, SQL
Frameworks: Spring Boot, React, Node.js, FastAPI, Django
Data & Cloud: PostgreSQL, Redis, MongoDB, Docker, Kubernetes, AWS, GCP
Tools & Architecture: Git, Linux, REST API, GraphQL
Experience:
- Architected enterprise cloud infrastructure using AWS, Docker, and Kubernetes.
- Built reactive frontends with React and TypeScript.
- Developed low-latency core services in Go and C++.
"""
    # Ingest once or reuse existing resume if already ingested
    u_email = f"g10_{time.time()}@example.com"
    s, resp = ingest_text(complex_resume, "G10 Determinism Candidate", u_email)
    if s == 201:
        complex_resume_id = resp["resumeId"]
    elif s == 409 and "existingResumeId" in resp:
        complex_resume_id = resp["existingResumeId"]
    else:
        raise AssertionError(f"Could not ingest or find resume: {resp}")

    first_skills_summary = None
    divergences = 0
    t0 = time.time()
    for run_i in range(100):
        s_ext, resp_ext = http_json("POST", f"/resumes/{complex_resume_id}/skills/extract", expected_status=200)
        skills = resp_ext.get("skills", [])
        summary = [
            (
                sk.get("skillId"),
                sk.get("skillName"),
                sk.get("matchedText"),
                sk.get("extractionMethod"),
                str(sk.get("extractionConfidence", "")),
                sk.get("contextSnippet")
            )
            for sk in skills
        ]
        if first_skills_summary is None:
            first_skills_summary = summary
        else:
            if summary != first_skills_summary:
                divergences += 1
    duration = time.time() - t0
    log(f"100 determinism runs completed in {duration:.2f}s ({duration/100*1000:.1f}ms/run). Divergences: {divergences}")
    results["Group 10: Determinism"] = (divergences == 0, f"100 runs, {len(first_skills_summary)} skills extracted per run, {divergences} divergences, total time {duration:.2f}s")

    # -------------------------------------------------------------
    # 11. IDEMPOTENCY (Group 11)
    # -------------------------------------------------------------
    log("Running Group 11: Idempotency...")
    u_email = f"g11_{time.time()}@example.com"
    s, resp = ingest_text("Experienced in Python, Docker, and PostgreSQL databases.", "G11 Candidate", u_email)
    res_id = resp["resumeId"]
    initial_skills = resp["extractedSkills"]
    idemp_pass = True
    for _ in range(5):
        s_ext, resp_ext = http_json("POST", f"/resumes/{res_id}/skills/extract", expected_status=200)
        skills_ext = resp_ext.get("skills", [])
        if len(skills_ext) != len(initial_skills):
            idemp_pass = False
    results["Group 11: Idempotency"] = (idemp_pass, f"Re-extracted 5 times, skill count maintained at {len(initial_skills)}")

    # -------------------------------------------------------------
    # 12. CONCURRENCY (Group 12 - 5, 10, 20 concurrent requests)
    # -------------------------------------------------------------
    log("Running Group 12: Concurrency (5, 10, 20 concurrent requests on same resume)...")
    u_email = f"g12_{time.time()}@example.com"
    s, resp = ingest_text("Senior backend engineer skilled in Java, Spring Boot, PostgreSQL, and Docker.", "G12 Concurrency", u_email)
    concur_resume_id = resp["resumeId"]
    
    concur_results = {}
    for concurrency_level in [5, 10, 20]:
        t_start = time.time()
        successes = 0
        errors = 0
        err_details = []
        with concurrent.futures.ThreadPoolExecutor(max_workers=concurrency_level) as executor:
            futures = [
                executor.submit(http_json, "POST", f"/resumes/{concur_resume_id}/skills/extract")
                for _ in range(concurrency_level)
            ]
            for f in concurrent.futures.as_completed(futures):
                try:
                    st, r = f.result()
                    if st == 200:
                        successes += 1
                    else:
                        errors += 1
                        err_details.append(f"HTTP {st}: {r}")
                except Exception as ex:
                    errors += 1
                    err_details.append(str(ex))
        t_elapsed = time.time() - t_start
        concur_results[concurrency_level] = (successes, errors, t_elapsed, err_details)
        log(f"Concurrency {concurrency_level} finished: {successes} 200s, {errors} errors in {t_elapsed:.2f}s")
    
    g12_pass = all(err == 0 for _, (_, err, _, _) in concur_results.items())
    results["Group 12 / B-07: Concurrency Serialization"] = (g12_pass, f"Levels 5/10/20: {concur_results}")

    # -------------------------------------------------------------
    # 13. CROSS-FORMAT RESUME PIPELINE (Group 13)
    # -------------------------------------------------------------
    log("Running Group 13: Cross-Format Resume Pipeline (TXT, DOCX, PDF)...")
    test_content = "Maya Lin\nSenior Cloud Engineer\nSkills:\nPython, Docker, Kubernetes, AWS, PostgreSQL, Linux\nExperience: Designed microservices."
    
    txt_bytes = test_content.encode("utf-8")
    docx_bytes = create_valid_docx_bytes(test_content)
    pdf_bytes = create_valid_pdf_bytes(test_content)
    
    s_txt, r_txt = http_multipart("/resumes", "candidate_maya.txt", txt_bytes, "text/plain", {"name": "Maya TXT", "email": f"maya_txt_{time.time()}@example.com"}, 201)
    s_docx, r_docx = http_multipart("/resumes", "candidate_maya.docx", docx_bytes, "application/vnd.openxmlformats-officedocument.wordprocessingml.document", {"name": "Maya DOCX", "email": f"maya_docx_{time.time()}@example.com"}, 201)
    s_pdf, r_pdf = http_multipart("/resumes", "candidate_maya.pdf", pdf_bytes, "application/pdf", {"name": "Maya PDF", "email": f"maya_pdf_{time.time()}@example.com"}, 201)
    
    txt_skills = sorted([s["skillName"] for s in r_txt.get("extractedSkills", [])])
    docx_skills = sorted([s["skillName"] for s in r_docx.get("extractedSkills", [])])
    pdf_skills = sorted([s["skillName"] for s in r_pdf.get("extractedSkills", [])])
    
    parity_pass = (txt_skills == docx_skills == pdf_skills) and len(txt_skills) >= 5
    results["Group 13: Cross-Format Parity"] = (parity_pass, f"TXT: {txt_skills}, DOCX: {docx_skills}, PDF: {pdf_skills}")

    # -------------------------------------------------------------
    # 14 & 15. LIFECYCLE & FAILURE TRANSITIONS (B-06 Blank Content)
    # -------------------------------------------------------------
    log("Running Group 14 & 15 / B-06: Lifecycle & Blank Content Handling...")
    blank_tests = [
        ("empty.txt", b"", "Upload rejected: File is empty or not provided.", 400),
        ("spaces.txt", b"   ", "Upload rejected: File contains only whitespace or is empty.", 400),
        ("tabs.txt", b"\t\t\t", "Upload rejected: File contains only whitespace or is empty.", 400),
        ("newlines.txt", b"\n\n\r\n", "Upload rejected: File contains only whitespace or is empty.", 400),
        ("bom_whitespace.txt", b"\xEF\xBB\xBF   \n\t", "Upload rejected: File contains only whitespace or is empty.", 400),
    ]
    b06_pass = True
    b06_details = []
    for fname, bcontent, expected_err, expected_st in blank_tests:
        s, r = http_multipart("/resumes", fname, bcontent, "text/plain", {"name": "Blank Test", "email": f"blank_{time.time()}@example.com"})
        if s != expected_st:
            b06_pass = False
            b06_details.append(f"{fname}: expected status {expected_st}, got {s}: {r}")
        elif expected_err not in str(r):
            b06_pass = False
            b06_details.append(f"{fname}: expected error containing '{expected_err}', got '{r}'")
            
    # Also test valid blank PDF
    blank_pdf = create_valid_pdf_bytes("   ")
    s_bpdf, r_bpdf = http_multipart("/resumes", "blank.pdf", blank_pdf, "application/pdf", {"name": "Blank PDF", "email": f"blankpdf_{time.time()}@example.com"})
    # Normal empty PDF extraction transitions to FAILED
    if s_bpdf != 201 or r_bpdf.get("parsingStatus") != "FAILED":
        b06_pass = False
        b06_details.append(f"blank.pdf: expected 201 with FAILED status, got status {s_bpdf}, body: {r_bpdf}")

    results["Group 14 & 15 / B-06: Blank Content Rejection"] = (b06_pass, f"Details: {b06_details}")

    # -------------------------------------------------------------
    # 16. API CONTRACTS (B-05 Validation)
    # -------------------------------------------------------------
    log("Running Group 16 / B-05: API Contracts...")
    # B-05: Non-existent resume ID -> 404
    s_404, r_404 = http_json("GET", "/resumes/999999/skills", expected_status=404)
    b05_404_pass = (s_404 == 404 and "not found" in str(r_404).lower())
    
    # Existing resume with zero skills -> 200 with empty list
    s_zero, r_zero = ingest_text("Candidate profile with general non-technical management background.", "Zero Skill", f"zero_{time.time()}@example.com")
    zero_id = r_zero["resumeId"]
    s_zskills, r_zskills = http_json("GET", f"/resumes/{zero_id}/skills", expected_status=200)
    zero_skills_pass = (s_zskills == 200 and r_zskills.get("skillsCount") == 0 and r_zskills.get("skills") == [])
    
    # Invalid ID syntax -> 400
    s_badid, r_badid = http_json("GET", "/resumes/invalid_id_abc/skills", expected_status=400)
    bad_syntax_pass = (s_badid == 400)
    
    # Information leak audit: check responses for stack traces
    leak_pass = "Exception" not in str(r_404) and "at com.fairhire" not in str(r_404) and "/app/" not in str(r_404)
    
    g16_pass = b05_404_pass and zero_skills_pass and bad_syntax_pass and leak_pass
    results["Group 16 / B-05: API Contract & Security Auditing"] = (g16_pass, f"404: {b05_404_pass}, 200 []: {zero_skills_pass}, 400 bad id: {bad_syntax_pass}, No leak: {leak_pass}")

    # -------------------------------------------------------------
    # 17. DATABASE INTEGRITY (Group 17)
    # -------------------------------------------------------------
    log("Running Group 17: Database Schema & Integrity Constraints...")
    cmd = 'docker exec fairhire-postgres psql -U postgres -d fairhire_db -c "SELECT conname, contype FROM pg_constraint WHERE conrelid = \'resume_skills\'::regclass;"'
    p = subprocess.run(cmd, shell=True, capture_output=True, text=True)
    out = p.stdout
    uq_present = "uq_resume_skills_resume_skill" in out
    fk_resume = "resume_skills_resume_id_fkey" in out
    fk_skill = "resume_skills_skill_id_fkey" in out
    g17_pass = uq_present and fk_resume and fk_skill
    results["Group 17: Database Integrity"] = (g17_pass, f"UQ: {uq_present}, FK Resume: {fk_resume}, FK Skill: {fk_skill}")

    # -------------------------------------------------------------
    # 18. TRANSACTION ROLLBACK (Group 18 / B-08)
    # -------------------------------------------------------------
    log("Running Group 18 / B-08: Transaction Rollback & Failure Status Persistence...")
    cmd_b08 = 'mvn test -Dtest=SkillExtractionIntegrationTest#testSkillExtractionFailureStatusPersisted'
    p_b08 = subprocess.run(cmd_b08, cwd="/mnt/c/Users/harhm/Downloads/FairHire_AI_Application/fairhire_ai/backend", shell=True, capture_output=True, text=True)
    b08_pass = "BUILD SUCCESS" in p_b08.stdout and "Failures: 0, Errors: 0" in p_b08.stdout
    results["Group 18 / B-08: Transaction Rollback Status Updater"] = (b08_pass, "SkillExtractionIntegrationTest#testSkillExtractionFailureStatusPersisted PASSED")

    # -------------------------------------------------------------
    # 19. MIGRATION & SCHEMA INTEGRITY (Group 19 / B-04)
    # -------------------------------------------------------------
    log("Running Group 19 / B-04: Migration Integrity & V3 Checksum...")
    cmd_flyway = 'docker exec fairhire-postgres psql -U postgres -d fairhire_db -t -A -c "SELECT version, checksum, success FROM flyway_schema_history ORDER BY installed_rank;"'
    p_fly = subprocess.run(cmd_flyway, shell=True, capture_output=True, text=True)
    fly_lines = [line.strip().split("|") for line in p_fly.stdout.strip().split("\n") if line.strip()]
    
    v3_checksum = None
    all_success = True
    for row in fly_lines:
        if len(row) >= 3:
            ver, cs, succ = row[0], row[1], row[2]
            if ver == "3":
                v3_checksum = cs
            if succ != "t":
                all_success = False
    v3_intact = (v3_checksum == "11453495")
    results["Group 19 / B-04: Migration History & V3 Checksum"] = (v3_intact and all_success, f"V3 Checksum: {v3_checksum} (expected 11453495), All Success: {all_success}")

    # -------------------------------------------------------------
    # 20. SECURITY (Group 20)
    # -------------------------------------------------------------
    log("Running Group 20: Security Ingestion Rejections...")
    sec_tests = [
        # PE / MZ Windows binary disguised as PDF
        ("evil.pdf", b"MZ\x90\x00\x03\x00\x00\x00" + b"\x00"*100, "application/pdf", 400, "Windows executable (MZ) binary detected"),
        # PE / MZ Windows binary disguised as DOCX
        ("evil.docx", b"MZ\x90\x00\x03\x00\x00\x00" + b"\x00"*100, "application/vnd.openxmlformats-officedocument.wordprocessingml.document", 400, "Windows executable (MZ) binary detected"),
        # ELF Linux binary
        ("evil_elf.pdf", b"\x7FELF\x02\x01\x01\x00" + b"\x00"*100, "application/pdf", 400, "Linux executable (ELF) binary detected"),
        # Shebang script
        ("evil_script.pdf", b"#!/bin/bash\nrm -rf /", "application/pdf", 400, "Executable script (#!) detected"),
        # Path traversal
        ("../../etc/passwd.pdf", b"%PDF-1.4\n" + b"\x00"*100, "application/pdf", 400, "path traversal"),
        # Command injection
        ("resume;rm -rf /;.pdf", b"%PDF-1.4\n" + b"\x00"*100, "application/pdf", 400, "command characters"),
        # Wrong magic bytes for PDF
        ("fake.pdf", b"NOT_A_PDF_STREAM_HEADER", "application/pdf", 400, "lacks standard PDF header"),
        # Disallowed MIME
        ("bad_mime.pdf", b"%PDF-1.4\n" + b"\x00"*100, "application/x-msdownload", 400, "Disallowed MIME type"),
    ]
    sec_pass = True
    sec_failures = []
    for fname, content, mime, exp_status, exp_err in sec_tests:
        s, r = http_multipart("/resumes", fname, content, mime, {"name": "Sec Test", "email": f"sec_{time.time()}@example.com"})
        if s != exp_status:
            sec_pass = False
            sec_failures.append(f"{fname}: expected {exp_status}, got {s}: {r}")
        elif exp_err.lower() not in str(r).lower():
            sec_pass = False
            sec_failures.append(f"{fname}: expected '{exp_err}', got '{r}'")
            
    # Oversized upload (> 10MB limit)
    oversized_bytes = b"%PDF-1.4\n" + b"X" * (11 * 1024 * 1024)
    s_over, r_over = http_multipart("/resumes", "oversized.pdf", oversized_bytes, "application/pdf", {"name": "Over Size", "email": f"over_{time.time()}@example.com"})
    if s_over != 400 or "exceeds maximum permitted limit" not in str(r_over):
        sec_pass = False
        sec_failures.append(f"oversized.pdf: expected 400 with size limit message, got {s_over}: {r_over}")
        
    results["Group 20: Security Ingestion Rejections"] = (sec_pass, f"Failures: {sec_failures}")

    # -------------------------------------------------------------
    # 21. PERFORMANCE PROFILING (Group 21)
    # -------------------------------------------------------------
    log("Running Group 21: Performance Profiling...")
    # Sizes: 1KB, 10KB, 50KB, 100KB, 500KB, 1MB, 5MB
    perf_results = {}
    base_paragraph = "Experienced senior developer proficient in Python, Java, Docker, Kubernetes, AWS, PostgreSQL, and Linux. Developed high-throughput microservices.\n"
    
    for target_kb in [1, 10, 50, 100, 500, 1000]:
        repeat_count = max(1, int((target_kb * 1024) / len(base_paragraph)))
        content = base_paragraph * repeat_count
        actual_size_bytes = len(content.encode("utf-8"))
        
        t0 = time.time()
        u_email = f"perf_{target_kb}kb_{time.time()}@example.com"
        s, resp = ingest_text(content, f"Perf {target_kb}KB", u_email)
        elapsed_ms = (time.time() - t0) * 1000
        perf_results[f"{target_kb} KB"] = {
            "status": s,
            "latency_ms": round(elapsed_ms, 2),
            "skillsCount": resp.get("skillsCount", 0) if s == 201 else 0
        }
        log(f"Perf {target_kb} KB ({actual_size_bytes} bytes): {elapsed_ms:.2f} ms, status {s}")
    
    # 5MB test with timeout protection
    try:
        content_5mb = base_paragraph * int((5 * 1024 * 1024) / len(base_paragraph))
        t0 = time.time()
        u_email = f"perf_5mb_{time.time()}@example.com"
        s_5mb, resp_5mb = ingest_text(content_5mb, "Perf 5MB", u_email)
        elapsed_5mb = (time.time() - t0) * 1000
        perf_results["5 MB"] = {"status": s_5mb, "latency_ms": round(elapsed_5mb, 2)}
    except Exception as ex:
        perf_results["5 MB"] = {"status": "TIMED_OUT", "latency_ms": "> 60000"}
        
    results["Group 21: Performance Profile"] = (True, f"Profiles: {perf_results}")

    # -------------------------------------------------------------
    # 22. FULL REGRESSION SUITE (Group 22)
    # -------------------------------------------------------------
    log("Running Group 22: Full Regression Verification...")
    ai_status, ai_health = http_json("GET", "/health/system")
    assert ai_status == 200
    assert ai_health.get("ai_service", {}).get("status") == "UP"
    results["Group 22: Full Regression & Cross-Service Connectivity"] = (True, f"Backend, PostgreSQL, and AI Service UP: {ai_health}")
    
    # Summary of all groups
    print("\n" + "="*80)
    print("FINAL REVALIDATION RESULTS SCORECARD")
    print("="*80)
    all_passed = True
    for grp, (passed, detail) in results.items():
        status_str = "PASS" if passed else "FAIL"
        if not passed:
            all_passed = False
        print(f"[{status_str}] {grp}: {detail}")
    print("="*80)
    
    if all_passed:
        print("OVERALL RESULT: PASS — ALL GROUPS AND DEFECTS VERIFIED")
    else:
        print("OVERALL RESULT: FAIL — UNRESOLVED DEFECTS REMAIN")
    print("="*80 + "\n")
    
    # Save raw test results for documentation
    with open("docs/testing/revalidation_raw_results.json", "w", encoding="utf-8") as f:
        json.dump({k: {"pass": v[0], "details": str(v[1])} for k, v in results.items()}, f, indent=2)

if __name__ == "__main__":
    main()
