import os
import json
import re
from collections import Counter, defaultdict

# File paths
SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
BASEWORDS_FILE = os.path.join(SCRIPT_DIR, "basewords.txt")
CORPUS_FILE = os.path.join(SCRIPT_DIR, "madura_corpus.txt")
OUTPUT_JSON = os.path.join(SCRIPT_DIR, "app/src/main/assets/madura_bigrams.json")

def clean_text(text):
    text = text.lower()
    # Keep only letters and Madura special chars
    text = re.sub(r'[^a-zâêôḍṭḷ\s]', '', text)
    return text

def train_models():
    if not os.path.exists(BASEWORDS_FILE) or not os.path.exists(CORPUS_FILE):
        print(f"File dictionary atau corpus tidak ditemukan!")
        return

    # 1. Load Valid Dictionary (Standard Words)
    with open(BASEWORDS_FILE, "r", encoding="utf-8") as f:
        valid_words = set(line.strip().lower() for line in f if line.strip())

    print(f"Dictionary dimuat: {len(valid_words)} kata baku.")

    # 2. Analyze Corpus for New Words & Bigrams
    with open(CORPUS_FILE, "r", encoding="utf-8") as f:
        content = f.read()

    sentences = re.split(r'[.\n!?]', content)
    
    raw_unigrams = Counter()
    for sentence in sentences:
        words = clean_text(sentence).split()
        for w in words:
            raw_unigrams[w] += 1

    # Learn a word if it's already valid OR if it appears frequently in the corpus
    # (Filters out one-off typos)
    learned_words = set()
    for word, count in raw_unigrams.items():
        if word in valid_words or count >= 2:
            learned_words.add(word)

    print(f"Total kata yang dianggap valid: {len(learned_words)} (termasuk kata baru dari cerita).")

    unigrams = Counter()
    bigrams = defaultdict(Counter)

    for sentence in sentences:
        words = clean_text(sentence).split()
        for i in range(len(words)):
            word = words[i]
            if word in learned_words:
                unigrams[word] += 1
                if i < len(words) - 1:
                    next_word = words[i+1]
                    if next_word in learned_words:
                        bigrams[word][next_word] += 1

    print("Proses penyaringan selesai.")

    # Convert to a more compact format for Java
    # Format: { "unigrams": { "word": freq }, "bigrams": { "word": ["next1", "next2", "next3"] } }
    trained_data = {
        "unigrams": dict(unigrams.most_common(5000)),
        "bigrams": {}
    }

    for word, next_words in bigrams.items():
        # Only take top 3 most likely next words
        top_next = [w for w, count in next_words.most_common(3)]
        trained_data["bigrams"][word] = top_next

    # Ensure assets folder exists
    os.makedirs(os.path.dirname(OUTPUT_JSON), exist_ok=True)
    
    with open(OUTPUT_JSON, "w", encoding="utf-8") as f:
        json.dump(trained_data, f, ensure_ascii=False, indent=4)
    
    print(f"Pelatihan Selesai!")
    print(f"Statistik: {len(unigrams)} kata unik, {len(bigrams)} pola kalimat ditemukan.")
    print(f"Model disimpan di: {OUTPUT_JSON}")

if __name__ == "__main__":
    train_models()
