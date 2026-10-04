// サーバーと同じ基準タイムゾーン（JST）での日時の各部分。
// toISOString() はUTCのため、日本時間0〜9時には前日になる
function partsInJapan(now: Date): Record<string, string> {
  return Object.fromEntries(
    new Intl.DateTimeFormat('en', {
      timeZone: 'Asia/Tokyo', year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit', hourCycle: 'h23',
    })
      .formatToParts(now)
      .map(({ type, value }) => [type, value]),
  )
}

// JSTでの今日の日付（YYYY-MM-DD）
export function todayInJapan(now: Date = new Date()): string {
  const p = partsInJapan(now)
  return `${p.year}-${p.month}-${p.day}`
}

// 予約枠の開始（YYYY-MM-DD + HH:mm[:ss]）がJSTの現在より後か。バックエンドの予約可否（RESERVATION-005）と同じ判定。
// 開始時刻は分単位（秒は常に00）のため、分単位で比較する
export function isUpcoming(date: string, startTime: string, now: Date = new Date()): boolean {
  const p = partsInJapan(now)
  return `${date}T${startTime.slice(0, 5)}` > `${p.year}-${p.month}-${p.day}T${p.hour}:${p.minute}`
}

// バックエンドは "HH:mm:ss" 形式で返すため表示用に秒を落とす
export const formatTime = (time: string) => time.slice(0, 5)

// YYYY-MM-DD 同士の日数差（今日=0）。どちらもUTC 0時として解釈するため端末のタイムゾーンに依存しない
export function daysFromToday(date: string): number {
  return Math.round((Date.parse(date) - Date.parse(todayInJapan())) / 86_400_000)
}
