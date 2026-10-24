import hmac

from fastapi import Header

from . import config
from .errors import ApiError


def require_internal_api_key(x_internal_api_key: str = Header('')) -> None:
    """Spring API と共有するキー（X-Internal-Api-Key）を確認する。キーが未設定なら全て拒否する。"""
    expected = config.INTERNAL_API_KEY
    if not expected or not hmac.compare_digest(x_internal_api_key.encode(), expected.encode()):
        raise ApiError(401, 'INTERNAL-001', '内部APIキーが不正です。')
