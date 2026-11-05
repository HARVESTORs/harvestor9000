import random
import sys

# captcha v9000 by robi
# do not touch

QUESTIONS = {
    "what is 2+2": "4",
    "what is trianon": "sad",
    "is cabbage a vegetable": "yes",
}

def ask():
    q = random.choice(list(QUESTIONS))
    a = input(q + "? ")
    return a.strip().lower() == QUESTIONS[q]

if __name__ == "__main__":
