//
//  Generated file. Do not edit.
//

import FlutterMacOS
import Foundation

import cloud_firestore
import firebase_auth
import firebase_core
import hotkey_manager_macos
import local_notifier
import screen_retriever_macos
import window_manager

func RegisterGeneratedPlugins(registry: FlutterPluginRegistry) {
  FLTFirebaseFirestorePlugin.register(with: registry.registrar(forPlugin: "FLTFirebaseFirestorePlugin"))
  FLTFirebaseAuthPlugin.register(with: registry.registrar(forPlugin: "FLTFirebaseAuthPlugin"))
  FLTFirebaseCorePlugin.register(with: registry.registrar(forPlugin: "FLTFirebaseCorePlugin"))
  HotkeyManagerMacosPlugin.register(with: registry.registrar(forPlugin: "HotkeyManagerMacosPlugin"))
  LocalNotifierPlugin.register(with: registry.registrar(forPlugin: "LocalNotifierPlugin"))
  ScreenRetrieverMacosPlugin.register(with: registry.registrar(forPlugin: "ScreenRetrieverMacosPlugin"))
  WindowManagerPlugin.register(with: registry.registrar(forPlugin: "WindowManagerPlugin"))
}
