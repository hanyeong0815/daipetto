export interface HealthRecord {
  healthRecordId: number
  weight?: number
  symptom?: string
  memo?: string
  recordedDate: string
}

// PATCHでは未指定(undefined)の項目は既存値を維持し、symptom/memoは空文字で消去される（docs/07 §6-3）
export interface HealthRecordRequest {
  weight?: number
  symptom?: string
  memo?: string
  recordedDate?: string
}
