import SwiftUI

struct SplashScreenView: View {
    var body: some View {
        ZStack {
            Color.white
                .ignoresSafeArea()

            VStack(spacing: 0) {
                ZStack {
                    RoundedRectangle(cornerRadius: 26)
                        .fill(Color(red: 0x6B / 255.0, green: 0x8F / 255.0, blue: 0x71 / 255.0).opacity(0.15))
                        .frame(width: 88, height: 88)

                    Text("💰")
                        .font(.system(size: 44))
                }

                Spacer().frame(height: 28)

                Text("PlzStop")
                    .font(.system(size: 28, weight: .bold))
                    .foregroundColor(Color(red: 0x1C / 255.0, green: 0x1B / 255.0, blue: 0x1F / 255.0))

                Spacer().frame(height: 8)

                Text("Track your spending.\nStop overspending.")
                    .font(.system(size: 16))
                    .foregroundColor(Color(red: 0x1C / 255.0, green: 0x1B / 255.0, blue: 0x1F / 255.0).opacity(0.6))
                    .multilineTextAlignment(.center)
            }
        }
    }
}
