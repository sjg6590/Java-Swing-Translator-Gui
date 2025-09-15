# Translator GUI Application

Java Swing app to capture a screen region, run OCR using manga-ocr (Python), and translate to English using DeepL.

## Setup

### Requirements
- Java 17+
- Maven 3.8+
- Python 3.9+ with manga-ocr installed and working
- DeepL API key (DEEPL_API_KEY)

### Environment
- Set env vars or use the in-app Settings dialog:
  - PYTHON_CMD: Path/command to Python with manga-ocr installed (e.g. python3 or a venv python)
  - DEEPL_API_KEY: Your DeepL API key

### Python virtual environment (.venv)
Create and use a project-local virtual environment:
```bash
python3 -m venv .venv
source .venv/bin/activate
pip install -U pip
pip install manga-ocr
```

### Build
```bash
mvn -DskipTests package
```

This creates a runnable shaded JAR at:
`target/translator-gui-application-0.1.0-shaded.jar`

### Run
```bash
DEEPL_API_KEY=your-key \
java -jar target/translator-gui-application-0.1.0-shaded.jar
```

Notes:
- The app auto-detects `.venv/bin/python` if `PYTHON_CMD` is not set.
- You can override with `PYTHON_CMD` env var or via App → Settings.

## Usage
- In the OCR tab, click "Select Screen Area" to draw a rectangle over manga text.
- OCR result appears in the OCR tab.
- Copy OCR text to the Translation tab, then click "Translate to English".
- Manage Python command and DeepL key in Settings.

## Notes
- OCR relies on `python -m manga_ocr <image>` behavior. Ensure manga-ocr works in your environment.
- Large selections may take longer.
- Temp images are deleted after OCR.
