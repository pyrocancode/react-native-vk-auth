require "json"

package = JSON.parse(File.read(File.join(__dir__, "package.json")))
folly_compiler_flags = '-DFOLLY_NO_CONFIG -DFOLLY_MOBILE=1 -DFOLLY_USE_LIBCPP=1 -Wno-comma -Wno-shorten-64-to-32'

Pod::Spec.new do |s|
  s.name         = "react-native-vk-auth"
  s.version      = package["version"]
  s.summary      = package["description"]
  s.homepage     = package["homepage"]
  s.license      = package["license"]
  s.authors      = package["author"]
  s.platforms    = { :ios => "15.1" }
  s.source       = { :git => "https://github.com/pyrocancode/react-native-vk-auth.git", :tag => "#{s.version}" }

  s.source_files = "ios/**/*.{h,m,mm,swift}"

  # VK ID SDK (OAuth 2.1). См. https://id.vk.com/about/business/go/docs/ru/vkid/latest/vk-id/connection/migration/ios/migration-ios
  s.dependency "VKID", "2.9.2"

  if ENV['RCT_NEW_ARCH_ENABLED'] == '1' then
    s.compiler_flags = folly_compiler_flags + " -DRCT_NEW_ARCH_ENABLED=1"
    s.pod_target_xcconfig = {
        "HEADER_SEARCH_PATHS" => "\"$(PODS_ROOT)/boost\"",
        "OTHER_CPLUSPLUSFLAGS" => "-DFOLLY_NO_CONFIG -DFOLLY_MOBILE=1 -DFOLLY_USE_LIBCPP=1",
        "CLANG_CXX_LANGUAGE_STANDARD" => "c++17"
    }

    unless respond_to?(:install_modules_dependencies, true)
      rn_pods = File.expand_path(File.join(__dir__, '..', '..', 'react-native', 'scripts', 'react_native_pods.rb'))
      require rn_pods if File.exist?(rn_pods)
    end

    if respond_to?(:install_modules_dependencies, true)
      install_modules_dependencies(s)
    else
      raise "[@pyrocancode/react-native-vk-auth] New Architecture requires React Native scripts (install_modules_dependencies). Check that react-native is in node_modules."
    end
  else
    s.dependency "React-Core"
  end
end
