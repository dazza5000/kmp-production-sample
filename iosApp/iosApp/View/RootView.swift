import SwiftUI
import RssReader

struct RootView: View {
    @EnvironmentObject var store: ObservableFeedStore
    @SwiftUI.State var errorMessage: String?
    
    var body: some View {
        ZStack {
            NavigationStack {
                MainFeedView()
            }.zIndex(0)
            if let errorMessage = self.errorMessage {
                VStack {
                    Spacer()
                    Text(errorMessage)
                        .foregroundColor(.white)
                        .padding(10.0)
                        .background(Color.black)
                        .cornerRadius(3.0)
                }
                .padding(.bottom, 10)
                .zIndex(1)
                .transition(.asymmetric(insertion: .move(edge: .bottom), removal: .opacity) )
            }
        }
    }
}
