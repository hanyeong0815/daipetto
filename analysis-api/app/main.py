from fastapi import FastAPI

from .errors import register_error_handlers
from .routers import internal

# Spring API からだけ呼ばれる内部API（フロントエンドは Spring の /api/v1/analysis/... を呼ぶ）。
# API文書（OpenAPI）は公開しない。必要なときは app.openapi() で出力する（docs/10 §6）
app = FastAPI(title='Daipetto Analysis API', docs_url=None, redoc_url=None, openapi_url=None)
register_error_handlers(app)
app.include_router(internal)
