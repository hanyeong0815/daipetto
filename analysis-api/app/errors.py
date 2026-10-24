from fastapi import FastAPI, Request
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse
from starlette.exceptions import HTTPException as StarletteHTTPException

# Spring API と同じエラー形式（docs/07 §2-4・§7-5）
_HTTP_ERRORS = {
    404: ('COMMON-001', '存在しないURLです。'),
    405: ('COMMON-002', '許可されていないHTTPメソッドです。'),
}
_SERVER_ERROR = (500, 'SERVER-001', 'サーバーでエラーが発生しました。時間をおいて再度お試しください。')
VALIDATION_ERROR = (400, 'VALIDATION-001', '入力内容を確認してください。')


class ApiError(Exception):
    def __init__(self, status: int, code: str, message: str):
        super().__init__(code)
        self.status, self.code, self.message = status, code, message


def error_response(status: int, code: str, message: str) -> JSONResponse:
    return JSONResponse({'success': False, 'data': None, 'code': code, 'message': message}, status_code=status)


def register_error_handlers(app: FastAPI) -> None:
    @app.exception_handler(ApiError)
    async def api_error(request: Request, exc: ApiError) -> JSONResponse:
        return error_response(exc.status, exc.code, exc.message)

    @app.exception_handler(RequestValidationError)
    async def validation_error(request: Request, exc: RequestValidationError) -> JSONResponse:
        return error_response(*VALIDATION_ERROR)

    @app.exception_handler(StarletteHTTPException)
    async def http_error(request: Request, exc: StarletteHTTPException) -> JSONResponse:
        if exc.status_code in _HTTP_ERRORS:
            return error_response(exc.status_code, *_HTTP_ERRORS[exc.status_code])
        return error_response(*_SERVER_ERROR)

    # 想定外の例外。トレースバックはサーバー（uvicorn）のログにだけ出し、応答には含めない
    @app.exception_handler(Exception)
    async def unexpected_error(request: Request, exc: Exception) -> JSONResponse:
        return error_response(*_SERVER_ERROR)
