FROM python:3.12-slim

WORKDIR /app
ENV TZ=Asia/Shanghai PYTHONUNBUFFERED=1

# 先拷依赖清单，利用 Docker 层缓存
COPY requirements.txt ./
RUN --mount=type=cache,target=/root/.cache/pip pip install --upgrade pip -i https://pypi.tuna.tsinghua.edu.cn/simple \
    && pip install -r requirements.txt -i https://pypi.tuna.tsinghua.edu.cn/simple

COPY --from=shared . ./shared
COPY app ./app
COPY data ./data

EXPOSE 8085

CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8085"]
