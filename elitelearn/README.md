# EliteLearn
### An AI-Powered Spaced Repetition & Active Recall Desktop Application

EliteLearn is a comprehensive desktop study tool designed to optimize learning by combining proven cognitive science frameworks with modern AI. Instead of passive reading, EliteLearn forces **active recall** through dynamic flashcards, AI-graded responses, and a unique "Feynman Mode" where the student must teach the AI.

---

## ✨ Inspiration
This project is built upon two highly effective, evidence-based learning methodologies:
1. **The Leitner System:** A spaced-repetition flashcard method. Instead of showing cards randomly, EliteLearn uses a **weighted probability algorithm** to show you the cards you struggle with more frequently, and the ones you know less often.
2. **The Feynman Technique:** The principle that the highest form of understanding is the ability to teach it. EliteLearn features a "Feynman Mode" where the AI acts as a confused student, prompting the user to explain concepts and asking probing, Socratic follow-up questions to expose gaps in understanding.

---

## 🏗️ Core Architecture Overview
EliteLearn is built using a strict **Object-Oriented design pattern** that cleanly separates concerns:
* **UI Layer:** Handled entirely by JavaFX. It utilizes background threads (`javafx.concurrent.Task`) to ensure the interface never freezes during heavy AI network requests or PDF generation.
* **Data Layer:** Uses custom weighted algorithms to simulate the Leitner spaced-repetition system and manages state for multi-turn AI conversations.
* **Service Layer:** Communicates with the Google Gemini API via HTTP for content generation and grading, and uses Apache PDFBox for dynamic, multi-page document generation.

---

## 🛠️ Tools & Technologies Used
* **Java 21 & JavaFX 21:** Core foundation for OOP logic and building a modern, custom-styled 1080p desktop GUI.
* **Google Gemini API:** Integrated via HTTP requests to generate flashcards, grade student answers, and conduct Socratic Feynman interviews.
* **Apache PDFBox:** Utilized to programmatically generate formatted, multi-page PDF tests and answer keys with a custom word-wrapping algorithm.
* **Google Gson:** Used for seamless JSON serialization/deserialization of deck data and AI API payloads.
* **Apache Maven:** Used for dependency management and project building.

---

## 📂 Project Structure & File Guide

```text
elitelearn/
├── src/
│   └── main/
│       ├── java/com/elitelearn/   <-- 📁 All Core Java Source Code
│       └── resources/             <-- 📁 Assets (styles.css, audio/)
├── pom.xml                        <-- ⚙️ Maven Build & Dependency Config
└── README.md                      <-- 📄 You are here!