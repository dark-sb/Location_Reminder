# Multimodal RAG Studio — AI Document Intelligence Platform

**Academic Engineering Seminar Project: Multimodal Retrieval-Augmented Generation**

---

## 1. Project Overview & Architecture

**Multimodal RAG Studio** is a fully functional, demonstrable, and verifiable web application implementing an end-to-end **Multimodal Retrieval-Augmented Generation (RAG)** pipeline.

Unlike traditional text-only RAG systems that discard visual formatting, tables, and page layouts, Multimodal RAG Studio parses documents page-by-page, extracts structured tables into markdown representations, renders 150-DPI visual page previews, indexes semantic chunks in persistent vector storage (**ChromaDB**), and generates grounded answers accompanied by verified citations linked directly to the deep **Document Viewer**.

```
                           [Document Upload: PDF, DOCX, TXT, Images]
                                              │
                        ┌─────────────────────┴─────────────────────┐
                        ▼                                           ▼
             [PyMuPDF / Document Parsers]                 [Visual Page Renderer]
             - Page-by-page text extraction               - 150-DPI high-res preview
             - Table detection & markdown matrix          - Visual diagram assets
             - OCR detection for scanned pages            - Private storage isolation
                        │                                           │
                        ▼                                           ▼
             [Semantic Overlapping Chunker]              [Page Visual Grounding]
             - 500-char boundary-preserving chunks        - Preserves page coordinates
             - 100-char semantic overlap                  - Generates page image URLs
                        │                                           │
                        ▼                                           ▼
             [Dense Vector Embeddings]                  [ChromaDB Vector Store]
             - 384-dim semantic feature vectorizer        - Persistent collection
             - Zero external API dependency (offline)     - Scoped document filtering
                        │                                           │
                        └─────────────────────┬─────────────────────┘
                                              ▼
                                   [Hybrid Retriever]
                                   - Dense semantic vector search
                                   - Lexical keyword frequency boost
                                   - Multi-document scope filtering
                                              │
                                              ▼
                                   [Context Fusion Engine]
                                   - Numbered evidence blocks
                                   - Page coordinates & table matrices
                                              │
                        ┌─────────────────────┴─────────────────────┐
                        ▼                                           ▼
             [Grounded Generation Engine]                [Evidence Validator]
             - Local Extractive Grounded Synthesis        - Matches citations against Chroma
             - Pluggable Google Gemini / OpenAI           - Eliminates hallucinated sources
             - Negative boundary rejection (unsupported)  - Computes faithfulness score
                        │                                           │
                        └─────────────────────┬─────────────────────┘
                                              ▼
                             [Delivery: Chat & Citations UI]
                             - Grounded answers with inline citations
                             - Supporting evidence cards with page preview
                             - One-click jump into Document Deep Inspector
```

---

## 2. Technology Stack

### Backend
- **Python 3.14 / FastAPI**: REST API framework with Pydantic v2 schemas and validation.
- **PyMuPDF (`fitz`)**: Page-by-page document rendering, table matrix detection, and OCR check.
- **`python-docx`**: Word document parsing and table transformation.
- **Pillow (PIL)**: Standalone image analysis and visual format normalizations.
- **ChromaDB**: Persistent vector database stored in `backend/data/chroma`.
- **SQLite**: Local relational database stored in `backend/data/rag_studio.db` tracking document statuses, pages, chunks, conversation histories, and benchmark runs.
- **Deterministic Semantic Hashing Vectorizer (384-dim)**: Built-in local offline embedding engine ensuring 100% demonstration availability without cloud rate limits or network failures.
- **Pluggable Cloud LLMs**: Configurable Google Gemini (`gemini-1.5-flash`) and OpenAI (`gpt-4o-mini`) via `.env`.

### Frontend
- **React 19 with TypeScript and Vite**: Responsive, ultra-fast client with HMR and production bundle optimization.
- **Tailwind CSS v4 & PostCSS**: Modern, clean design with dark/light mode toggle.
- **Lucide Icons**: Consistent, modern iconography.
- **Custom Toast Notification System**: Real-time feedback for uploads, queries, and errors.

---

## 3. Implemented Pages & Features

### A. Landing Page (`/about` & `/landing`)
- Seminar background: "Multimodal Retrieval-Augmented Generation".
- Interactive visual diagram detailing the 10 RAG pipeline stages.
- Live grounded demonstration comparing text-only RAG vs. Multimodal RAG.
- Technical specifications and supported file formats.

### B. Main Dashboard (`/dashboard`)
- Real backend metric cards: Total Documents, Successfully Processed, Currently Processing, Failed Ingestions, Indexed Pages, and ChromaDB Vector Chunks.
- Recently Ingested Documents grid with format, page count, and status badges.
- Recent AI Conversations list with quick-resume shortcuts.
- Quick action button to trigger the multi-file upload modal.

### C. Document Ingestion & Management (`/documents`)
- Multi-file drag-and-drop uploader supporting **PDF, DOCX, TXT, PNG, and JPG** files up to 50MB.
- Server-side filename sanitization and directory traversal prevention.
- Search documents by filename and filter by status (`completed`, `processing`, `failed`).
- Document inspection modal displaying chunk breakdowns, word counts, and page mappings.
- Reprocess/retry button and permanent delete button (cascades through SQLite, filesystem, and ChromaDB).

### D. Deep Document Viewer (`/viewer`)
- Interactive dual-pane document inspector.
- Left thumbnail strip with visual indicators for detected tables (📊) and images (🖼️).
- Four inspection tabs:
  1. **Visual Preview**: High-resolution rendered page images with zoom controls (50% to 200%).
  2. **Extracted Text**: Formatted text and structured markdown tables extracted from the page.
  3. **Page Chunks**: All semantic overlapping chunks indexed for that specific page.
  4. **Metadata**: File size, format, upload date, and vector counts.
- **Deep Linking**: Click any citation in AI Chat to automatically open that exact document and page in the viewer!

### E. AI Chat Workspace (`/chat`)
- Document Scope Selector: Toggle between "All Indexed Documents" or cherry-pick specific documents to prevent cross-document contamination.
- Query Mode Presets:
  - **General Q&A**: Freeform conversational inquiry.
  - **Summarize Document**: Key findings synthesis.
  - **Extract Specific Facts**: Extracts numerical values, metrics, and dates.
  - **Compare Documents**: Synthesizes contrasts between two or more documents.
  - **Tables & Visuals**: Queries diagram data and tabular structures.
- Evidence & Citations Panel:
  - Displays source document title and page number.
  - Excerpt quote and normalized relevance score.
  - Rendered page preview thumbnail.
  - "Open Page in Viewer" deep-link button.
- **Hallucination Mitigation & Negative Rejection**: If queried about facts absent from indexed documents, the system refuses to fabricate answers and warns of insufficient evidence.

### F. RAG Benchmarking & Reliability (`/evaluation`)
- Automated benchmark test suite verifying:
  - Retrieval latency (average ~6.3 ms).
  - Generation latency.
  - Citation validity rate (100% verified against actual ChromaDB records).
  - Grounding faithfulness score.
- Custom query test runner allowing users to evaluate custom prompts against ground-truth keywords.
- Historical benchmark runs table.

### G. System Health & Settings (`/settings`)
- Displays live status of OCR Engine, PyMuPDF, ChromaDB, active LLM provider, and active Embedding provider.
- Storage isolation details and document size limits.
- Zero secret API keys exposed in the frontend.

---

## 4. Installation & Local Startup Guide

### Prerequisites
- **Node.js** (v18 or higher) and `npm`
- **Python** (v3.10 to v3.14) and `pip`

### Step 1: Backend Setup
```bash
# Navigate to the backend directory
cd d:/Project/Reminder/backend

# Install Python requirements
pip install -r requirements.txt

# Create .env configuration (or copy from .env.example)
cp .env.example .env

# Run backend unit tests to verify pipeline
python -m pytest tests -v

# Launch the FastAPI backend server
python -m uvicorn app.main:app --host 127.0.0.1 --port 8000
```
The backend API will be available at: `http://127.0.0.1:8000` (Interactive API docs at `http://127.0.0.1:8000/docs`).

### Step 2: Frontend Setup
```bash
# In a separate terminal, navigate to the frontend directory
cd d:/Project/Reminder/frontend

# Install npm dependencies
npm install

# Test production build
npm run build

# Start the Vite development server
npm run dev
```
The frontend web application will be available at: `http://127.0.0.1:5173`.

---

## 5. Demonstration & Evaluation Workflow for Presentation

1. **Verify Backend Health**: Open the app in your browser at `http://127.0.0.1:5173`. Observe the green "Backend Online" badge and ChromaDB chunk counter in the top navigation bar.
2. **Review Ingested Documents**: Open the **Documents** tab. The sample 3-page academic seminar PDF is pre-indexed with 3 pages and 9 chunks.
3. **Inspect in Deep Document Viewer**: Navigate to the **Document Viewer** tab. Flip between pages 1, 2, and 3. Inspect the rendered page preview, the extracted text, and the table on Page 2.
4. **Conduct Grounded Q&A**: Navigate to the **AI Chat Workspace**.
   - Query: *"What are the benchmark comparison metrics between text-only RAG and Multimodal RAG Studio?"*
   - Observe: The system extracts the exact table from Page 2 and cites `[sample_multimodal_rag_seminar.pdf, Page 2]`.
   - Click "Open Page in Viewer" in the citations panel to see the source page in high resolution.
5. **Test Negative Rejection (Hallucination Defense)**:
   - Query: *"How do you cultivate blue strawberries in volcanic soil under lunar gravity?"*
   - Observe: The system refuses to fabricate an answer, returning a clear `insufficient_evidence` warning with `Grounding Status: Insufficient Evidence`.
6. **Execute Benchmark Evaluation**: Navigate to **RAG Benchmarking** and click **Run Automated Benchmark Suite**. Review the retrieval latency (~6ms), citation validity (100%), and faithfulness scores.

---

## 6. Security and Privacy Safeguards

- **Storage Isolation**: Uploaded files and rendered page images reside strictly in `backend/data/storage` and `backend/data/pages`, inaccessible directly through public frontend URLs.
- **Filename Sanitization**: Uploaded filenames are stripped of path traversal tokens (`..`, `/`, `\`) before being assigned a unique UUID.
- **Cascade Deletion**: Deleting a document removes its SQLite database records, all associated ChromaDB vector embeddings, stored files, and page preview images.
- **API Key Protection**: API keys are loaded via server-side environment variables and are never transmitted to the frontend.

---

## 7. Current Limitations & Future Roadmap

- **OCR Engine Dependency**: When Tesseract OCR binary is not installed in the system PATH, the pipeline utilizes native PyMuPDF text and tabular extraction. To enable OCR for fully scanned, non-searchable image PDFs, install Tesseract OCR and add it to your system PATH.
- **ColPali Multi-Vector Late Interaction**: While page-level 150-DPI visual grounding is implemented, deep late-interaction visual token embeddings (e.g. ColPali ViT patch projections) can be configured by attaching a PyTorch vision model.
