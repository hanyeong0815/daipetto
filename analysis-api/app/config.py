import os
from pathlib import Path

# ローカル実行時は analysis-api/.env を読み込む（Docker では compose の environment が優先される）
_env_file = Path(__file__).resolve().parent.parent / '.env'
if _env_file.exists():
    for _line in _env_file.read_text(encoding='utf-8').splitlines():
        _key, _sep, _value = _line.partition('=')
        if _sep and not _key.strip().startswith('#'):
            os.environ.setdefault(_key.strip(), _value.strip())

DB = {
    'host': os.environ.get('DB_HOST', 'localhost'),
    'port': os.environ.get('DB_PORT', '5432'),
    'dbname': os.environ.get('DB_NAME', 'daipetto'),
    'user': os.environ.get('DB_USERNAME', ''),
    'password': os.environ.get('DB_PASSWORD', ''),
}
INTERNAL_API_KEY = os.environ.get('INTERNAL_API_KEY', '')
