export function requestNotificationPermission() {
  if (!('Notification' in window)) return 'denied'
  if (Notification.permission === 'granted') return 'granted'
  if (Notification.permission === 'denied') return 'denied'
  return 'default'
}

export async function askPermission() {
  if (!('Notification' in window)) return false
  if (Notification.permission === 'granted') return true
  const result = await Notification.requestPermission()
  return result === 'granted'
}

export function sendNotification(title, body) {
  if (Notification.permission !== 'granted') return
  new Notification(title, { body, icon: '/favicon.ico', tag: 'checkin-reminder' })
}

const LAST_NOTIFY_KEY = 'shiguang-last-notify-date'
export function shouldNotifyToday() {
  const last = localStorage.getItem(LAST_NOTIFY_KEY)
  const today = new Date().toDateString()
  if (last === today) return false
  localStorage.setItem(LAST_NOTIFY_KEY, today)
  return true
}
