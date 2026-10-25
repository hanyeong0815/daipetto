"""docs/*.md を Notion へ反映するための Markdown に変換する（.ai-collab/SETUP.md §6）。

- 先頭行（`# 07_API_Design`）を除く。ページタイトルは Notion のページ側にある
- コード以外の `<タグ>` 形式の文字をエスケープする
- 他の設計書への相対リンク（`[06_ERD](06_ERD%20<id>.md)`）を Notion のページメンションにする
- CommonMark で太字にならない `**...**`（例: `）**とする`）を警告する

使い方（リポジトリ直下）: python scripts/notion_prep.py <出力フォルダ>
出力: <id>.md（ページID = ファイル名のハッシュ）と manifest.json
"""
import glob
import json
import re
import sys
import unicodedata
from pathlib import Path

TAG = re.compile(r'<(?=[A-Za-z/!])')
DOC_LINK = re.compile(r'\[([^\]]+)\]\(\d\d_[A-Za-z_]+%20([0-9a-f]{32})\.md\)')
BOLD = re.compile(r'\*\*(.+?)\*\*')


def is_punct(ch):
    return ch != '' and unicodedata.category(ch).startswith('P')


def is_space(ch):
    return ch == '' or ch.isspace()


def unrendered_bold(text):
    for m in BOLD.finditer(text):
        s, e = m.start(), m.end()
        before, after_open = text[s - 1] if s else '', text[s + 2]
        before_close, after = text[e - 3], text[e] if e < len(text) else ''
        left = not is_space(after_open) and (not is_punct(after_open) or is_space(before) or is_punct(before))
        right = not is_space(before_close) and (not is_punct(before_close) or is_space(after) or is_punct(after))
        if not (left and right):
            yield m.group()


def convert_line(line):
    parts = line.split('`')
    for i in range(0, len(parts), 2):  # 偶数番目がインラインコードの外
        parts[i] = TAG.sub(r'\\<', parts[i])
        parts[i] = DOC_LINK.sub(lambda m: f'<mention url="https://app.notion.com/p/{m.group(2)}">{m.group(1)}</mention>', parts[i])
    return '`'.join(parts)


def main(out):
    out.mkdir(parents=True, exist_ok=True)
    manifest, warnings = [], []
    for path in sorted(glob.glob('docs/[0-9][0-9]_*.md')):
        title, page_id = Path(path).stem.rsplit(' ', 1)
        lines = Path(path).read_text(encoding='utf-8').splitlines()
        body, in_code = [], False
        for no, line in enumerate(lines[1:], 2):
            if line.lstrip().startswith('```'):
                in_code = not in_code
                body.append(line)
                continue
            if not in_code:
                masked = re.sub(r'`[^`]*`', lambda m: 'x' * len(m.group()), line)
                warnings += [f'{title}:{no}: {b}' for b in unrendered_bold(masked)]
                line = convert_line(line)
            body.append(line)
        text = '\n'.join(body).strip('\n') + '\n'
        (out / f'{page_id}.md').write_text(text, encoding='utf-8')
        manifest.append({'id': page_id, 'title': title, 'chars': len(text)})
    (out / 'manifest.json').write_text(json.dumps(manifest, ensure_ascii=False, indent=2), encoding='utf-8')
    for m in manifest:
        print(m)
    for w in warnings:
        print('WARN bold not rendered:', w)


if __name__ == '__main__':
    main(Path(sys.argv[1]))
