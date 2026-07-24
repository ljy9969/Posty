import WidgetKit
import SwiftUI

// Kotlin 앱이 App Group 공유 UserDefaults 에 저장한 tasks_json 을 읽어 보여준다.
private let appGroup = "group.com.unboundapex.posty"
private let cream = Color(red: 1.0, green: 0.992, blue: 0.969)
private let mintDark = Color(red: 0.169, green: 0.702, blue: 0.667)
private let ink = Color(red: 0.227, green: 0.227, blue: 0.227)

// 스티키 색 팔레트(Compose StickyPalette 의 종이색과 동일).
private let paper: [Color] = [
    Color(red: 1.0, green: 0.953, blue: 0.690),
    Color(red: 0.780, green: 0.941, blue: 0.859),
    Color(red: 1.0, green: 0.847, blue: 0.800),
    Color(red: 0.839, green: 0.910, blue: 1.0),
    Color(red: 0.910, green: 0.875, blue: 0.961),
    Color(red: 1.0, green: 0.890, blue: 0.945),
]
private func paperColor(_ i: Int) -> Color { paper[((i % paper.count) + paper.count) % paper.count] }

// kotlinx.serialization(encodeDefaults=false) 출력 — 기본값 필드는 생략되므로 모두 옵셔널.
private struct TaskDTO: Decodable {
    let id: String
    let text: String
    let dueDate: Int64?
    let completedAt: Int64?
    let order: Int?
    let colorIndex: Int?
    let pinned: Bool?
}

struct WItem: Identifiable {
    let id: String
    let text: String
    let due: String?
    let colorIndex: Int
}

struct PostyEntry: TimelineEntry {
    let date: Date
    let items: [WItem]
    let total: Int
}

private func loadEntry() -> PostyEntry {
    let json = UserDefaults(suiteName: appGroup)?.string(forKey: "tasks_json") ?? "[]"
    let tasks = (try? JSONDecoder().decode([TaskDTO].self, from: Data(json.utf8))) ?? []
    let active = tasks
        .filter { $0.completedAt == nil }
        .sorted { a, b in
            let pa = a.pinned ?? false, pb = b.pinned ?? false
            if pa != pb { return pa }            // 고정(pinned) 우선
            return (a.order ?? 0) < (b.order ?? 0)
        }
    let items = active.prefix(10).map {
        WItem(id: $0.id, text: $0.text, due: dueLabel($0.dueDate), colorIndex: $0.colorIndex ?? 0)
    }
    return PostyEntry(date: Date(), items: Array(items), total: active.count)
}

/** 마감 D-day 라벨(Kotlin dueLabel 과 동일 규칙). dueDate 는 UTC 자정 millis. */
private func dueLabel(_ millis: Int64?) -> String? {
    guard let millis = millis else { return nil }
    var utc = Calendar(identifier: .gregorian)
    utc.timeZone = TimeZone(secondsFromGMT: 0)!
    let date = Date(timeIntervalSince1970: Double(millis) / 1000.0)
    let c = utc.dateComponents([.year, .month, .day], from: date)
    let local = Calendar(identifier: .gregorian)
    guard let dueLocal = local.date(from: DateComponents(year: c.year, month: c.month, day: c.day)) else { return nil }
    let today = local.startOfDay(for: Date())
    let days = local.dateComponents([.day], from: today, to: local.startOfDay(for: dueLocal)).day ?? 0
    switch days {
    case 0: return "Due today"
    case 1: return "Due tomorrow"
    case let d where d > 1: return "D-\(d)"
    case -1: return "1 day overdue"
    default: return "\(-days) days overdue"
    }
}

struct Provider: TimelineProvider {
    func placeholder(in context: Context) -> PostyEntry {
        PostyEntry(date: Date(), items: [
            WItem(id: "1", text: "Today's task", due: nil, colorIndex: 1),
            WItem(id: "2", text: "Next task", due: "D-2", colorIndex: 2),
        ], total: 3)
    }
    func getSnapshot(in context: Context, completion: @escaping (PostyEntry) -> Void) {
        completion(loadEntry())
    }
    func getTimeline(in context: Context, completion: @escaping (Timeline<PostyEntry>) -> Void) {
        let entry = loadEntry()
        // D-day 가 날짜 경과로 바뀔 수 있어 1시간마다 갱신(앱 변경 시엔 즉시 reload).
        let next = Calendar.current.date(byAdding: .hour, value: 1, to: Date()) ?? Date().addingTimeInterval(3600)
        completion(Timeline(entries: [entry], policy: .after(next)))
    }
}

// MARK: - Views

private struct Brand: View {
    var body: some View {
        HStack(spacing: 4) {
            Image(systemName: "checkmark.seal.fill").foregroundColor(mintDark).font(.caption2)
            Text("Posty").font(.caption2).foregroundColor(.secondary)
        }
    }
}

private struct Row: View {
    let item: WItem
    var body: some View {
        HStack(spacing: 8) {
            RoundedRectangle(cornerRadius: 2)
                .fill(paperColor(item.colorIndex))
                .frame(width: 5, height: 28)
            VStack(alignment: .leading, spacing: 1) {
                Text(item.text).font(.system(size: 14, weight: .medium)).foregroundColor(ink).lineLimit(1)
                if let due = item.due {
                    Text(due).font(.caption2).foregroundColor(mintDark)
                }
            }
            Spacer(minLength: 0)
        }
    }
}

private struct EmptyView2: View {
    var body: some View {
        VStack(spacing: 6) {
            Brand()
            Spacer()
            Text("All clear :)").font(.subheadline).foregroundColor(.secondary)
            Spacer()
        }.frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .leading)
    }
}

struct PostyWidgetEntryView: View {
    @Environment(\.widgetFamily) private var family
    var entry: PostyEntry

    var body: some View {
        Group {
            if entry.items.isEmpty {
                EmptyView2()
            } else {
                switch family {
                case .systemLarge: largeBody
                case .systemMedium: mediumBody
                default: smallBody
                }
            }
        }
        .padding(14)
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .leading)
        .widgetCream()
    }

    private var top: WItem { entry.items[0] }

    private var smallBody: some View {
        VStack(alignment: .leading, spacing: 6) {
            Brand()
            Spacer(minLength: 2)
            Text(top.text).font(.system(size: 17, weight: .semibold)).foregroundColor(ink).lineLimit(3)
            if let due = top.due { Text(due).font(.caption).foregroundColor(mintDark) }
            Spacer(minLength: 2)
            if entry.total > 1 { Text("+\(entry.total - 1) more").font(.caption2).foregroundColor(.secondary) }
        }
    }

    private var mediumBody: some View {
        VStack(alignment: .leading, spacing: 6) {
            Brand()
            Spacer(minLength: 2)
            Text(top.text).font(.system(size: 18, weight: .semibold)).foregroundColor(ink).lineLimit(2)
            if let due = top.due { Text(due).font(.caption).foregroundColor(mintDark) }
            Spacer(minLength: 2)
            if entry.total > 1 { Text("+\(entry.total - 1) more").font(.caption2).foregroundColor(.secondary) }
        }
    }

    private var largeBody: some View {
        let shown = Array(entry.items.prefix(7))
        return VStack(alignment: .leading, spacing: 8) {
            HStack {
                Brand()
                Spacer()
                Text("\(entry.total) notes").font(.caption2).foregroundColor(.secondary)
            }
            ForEach(shown) { Row(item: $0) }
            if entry.total > shown.count {
                Text("+\(entry.total - shown.count) more").font(.caption2).foregroundColor(.secondary)
            }
            Spacer(minLength: 0)
        }
    }
}

private extension View {
    // iOS 17+ 는 containerBackground 필수, 이전은 background 로 폴백.
    @ViewBuilder
    func widgetCream() -> some View {
        if #available(iOS 17.0, *) {
            self.containerBackground(cream, for: .widget)
        } else {
            self.background(cream)
        }
    }
}

@main
struct PostyWidget: Widget {
    let kind = "PostyWidget"
    var body: some WidgetConfiguration {
        StaticConfiguration(kind: kind, provider: Provider()) { entry in
            PostyWidgetEntryView(entry: entry)
        }
        .configurationDisplayName("Posty")
        .description("Your top tasks at a glance")
        .supportedFamilies([.systemSmall, .systemMedium, .systemLarge])
    }
}
