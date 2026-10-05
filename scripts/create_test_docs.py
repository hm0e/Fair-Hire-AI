import zipfile
import io
import os

def create_valid_pdf(filepath, text):
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

    with open(filepath, "wb") as f:
        f.write(header + obj1 + obj2 + obj3 + obj4 + obj5 + xref + trailer)
    print(f"Created PDF: {filepath}")

def create_valid_docx(filepath, text):
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

    with zipfile.ZipFile(filepath, "w", zipfile.ZIP_DEFLATED) as zf:
        zf.writestr("[Content_Types].xml", content_types)
        zf.writestr("_rels/.rels", rels)
        zf.writestr("word/document.xml", doc_xml)
    print(f"Created DOCX: {filepath}")

def create_valid_txt(filepath, text):
    with open(filepath, "w", encoding="utf-8") as f:
        f.write(text)
    print(f"Created TXT: {filepath}")

if __name__ == "__main__":
    os.makedirs("test_samples", exist_ok=True)
    resume_text = """Dr. Elena Vance
Senior AI Research Scientist
Email: elena.vance@example.com

Summary:
Over 8 years of research experience in Natural Language Processing and algorithmic fairness.

Skills:
Python, PyTorch, Transformers, Java, Spring Boot, PostgreSQL, Docker

Experience:
Senior Research Scientist at FairAI Labs (2021 - Present)
- Designed interpretable transformer evaluation pipelines.
- Published 4 peer-reviewed conference papers on algorithmic debiasing."""

    create_valid_pdf("test_samples/sample_resume.pdf", resume_text)
    create_valid_docx("test_samples/sample_resume.docx", resume_text)
    create_valid_txt("test_samples/sample_resume.txt", resume_text)
