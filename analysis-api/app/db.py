from collections.abc import Iterator

import psycopg

from . import config


def get_db() -> Iterator[psycopg.Connection]:
    """リクエストごとに接続する。テーブルの正は Spring の Flyway で、この API は読み取りのみ（DB側でも書き込みを禁止）。"""
    # ponytail: リクエストごとの接続。呼び出しが増えたら psycopg_pool に切り替える
    with psycopg.connect(**config.DB, autocommit=True, connect_timeout=5,
                         options='-c default_transaction_read_only=on') as conn:
        yield conn
