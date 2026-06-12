.PHONY: install-ai install-ai-dev run-ai check-ai export-ai-reqs

AI_DIR := services/ai-agent

install-ai:
	cd $(AI_DIR) && poetry install --no-root

install-ai-dev:
	cd $(AI_DIR) && poetry install --no-root --with dev

run-ai:
	cd $(AI_DIR) && poetry run python manage.py runserver

check-ai:
	cd $(AI_DIR) && poetry run python manage.py check

export-ai-reqs:
	cd $(AI_DIR) && poetry export -f requirements.txt --output requirements.txt --without-hashes --without dev
