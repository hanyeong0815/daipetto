// サーバーと同じ基準タイムゾーン（JST）での今日の日付（YYYY-MM-DD）。
// toISOString() はUTCのため、日本時間0〜9時には前日になる
export function todayInJapan(): string {
  const parts = Object.fromEntries(
    new Intl.DateTimeFormat('en', { timeZone: 'Asia/Tokyo', year: 'numeric', month: '2-digit', day: '2-digit' })
      .formatToParts(new Date())
      .map(({ type, value }) => [type, value]),
  )
  return `${parts.year}-${parts.month}-${parts.day}`
}
