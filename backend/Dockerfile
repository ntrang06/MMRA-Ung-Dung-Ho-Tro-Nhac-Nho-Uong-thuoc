
# MMRA FastAPI - Google Cloud Run
FROM python:3.12-slim

ENV PYTHONDONTWRITEBYTECODE=1
ENV PYTHONUNBUFFERED=1
ENV PIP_NO_CACHE_DIR=1

WORKDIR /app

# Cài đặt thư viện trước để Docker cache bước này.
COPY requirements.txt /app/requirements.txt

RUN pip install --no-cache-dir \
    -r /app/requirements.txt

# Chỉ sao chép mã Backend cần chạy.
COPY app /app/app
COPY migrations /app/migrations
COPY alembic.ini /app/alembic.ini

# Không chạy dịch vụ dưới quyền root.
RUN useradd --create-home --uid 10001 mmra \
    && chown -R mmra:mmra /app

USER mmra

# Cloud Run cung cấp biến PORT.
EXPOSE 8080

CMD ["sh", "-c", "exec uvicorn app.main:app --host 0.0.0.0 --port ${PORT:-8080}"]
