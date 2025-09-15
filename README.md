# Translator GUI Application

Java Swing app to capture a screen region, run OCR using manga-ocr (Python), and translate to English using DeepL.

## Setup

### Requirements
- Java 17+
- Maven 3.8+
- Python 3.9+ with manga-ocr installed and working (for Japanese manga)
- Tesseract OCR installed (for non-Japanese languages like Portuguese, English, etc.)
- DeepL API key (DEEPL_API_KEY)

### Environment
- Set env vars or use the in-app Settings dialog:
  - PYTHON_CMD: Path/command to Python with manga-ocr installed (e.g. python3 or a venv python)
  - TESSERACT_CMD: Path/command to Tesseract OCR (optional, defaults to "tesseract")
  - DEEPL_API_KEY: Your DeepL API key

### Python virtual environment (.venv)
Create and use a project-local virtual environment:
```bash
python3 -m venv .venv
source .venv/bin/activate
pip install -U pip
pip install manga-ocr
```

### Tesseract OCR Installation
Install Tesseract OCR for non-Japanese language support:

**macOS:**
```bash
brew install tesseract
```

**Ubuntu/Debian:**
```bash
sudo apt-get install tesseract-ocr
sudo apt-get install tesseract-ocr-por  # For Portuguese
sudo apt-get install tesseract-ocr-spa  # For Spanish
sudo apt-get install tesseract-ocr-fra  # For French
sudo apt-get install tesseract-ocr-deu  # For German
```

**Windows:**
Download and install from: https://github.com/UB-Mannheim/tesseract/wiki

### Build
```bash
mvn -DskipTests package
```

This creates a runnable JAR at:
`target/translator-gui-application-0.1.0.jar`

### Run
```bash
DEEPL_API_KEY=your-key \
java -jar target/translator-gui-application-0.1.0.jar
```

Notes:
- The app auto-detects `.venv/bin/python` if `PYTHON_CMD` is not set.
- You can override with `PYTHON_CMD` env var or via App → Settings.

## Usage
- In the OCR tab, click "Select Screen Area" to draw a rectangle over manga text.
- OCR result appears in the OCR tab.
- Copy OCR text to the Translation tab, then click "Translate to English".
- Manage Python command, DeepL key, and OCR language in Settings.

### Language Selection
- **Japanese manga**: Use "Japanese (manga-ocr)" - requires manga-ocr Python package
- **Portuguese manga**: Use "Portuguese (Tesseract)" - requires Tesseract with Portuguese language pack
- **Other languages**: Select appropriate Tesseract option - requires Tesseract with corresponding language pack

## Notes
- Large selections may take longer.
- Temp images are deleted after OCR.
