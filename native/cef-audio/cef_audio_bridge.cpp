#include "include/cef_browser.h"

#if defined(_WIN32)
#define ENOUGH_EXPORT extern "C" __declspec(dllexport)
#else
#define ENOUGH_EXPORT extern "C" __attribute__((visibility("default")))
#endif

// JCEF stores the raw CefBrowser* in CefNativeAdapter.N_CefHandle.
ENOUGH_EXPORT void enough_cef_set_audio_muted(void* handle, int muted) {
    auto* raw_browser = reinterpret_cast<CefBrowser*>(handle);
    if (!raw_browser) return;
    CefRefPtr<CefBrowser> browser(raw_browser);
    CefRefPtr<CefBrowserHost> host = browser->GetHost();
    if (host) host->SetAudioMuted(muted != 0);
}
