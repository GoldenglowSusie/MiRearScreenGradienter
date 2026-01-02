allprojects {
    repositories {
        google()
        mavenCentral()
    }
}

val newBuildDir: Directory = rootProject.layout.buildDirectory.dir("../../build").get()
rootProject.layout.buildDirectory.value(newBuildDir)

subprojects {
    val newSubprojectBuildDir: Directory = newBuildDir.dir(project.name)
    project.layout.buildDirectory.value(newSubprojectBuildDir)
    
    // 为所有子项目（包括插件）提供默认的 compileSdk 配置
    // 这解决了 Flutter 插件无法访问 flutter 对象的问题
    afterEvaluate {
        if (project.hasProperty("android")) {
            try {
                // 使用 Groovy 兼容的方式设置 compileSdkVersion
                val android = project.extensions.getByName("android")
                if (android != null) {
                    // 尝试通过反射设置 compileSdkVersion
                    val androidClass = android.javaClass
                    try {
                        // 检查是否已有 compileSdkVersion
                        val getCompileSdkVersion = androidClass.methods.find { 
                            it.name == "getCompileSdkVersion" || it.name == "getCompileSdk"
                        }
                        if (getCompileSdkVersion != null) {
                            val currentSdk = getCompileSdkVersion.invoke(android)
                            if (currentSdk == null) {
                                // 设置 compileSdkVersion
                                val setCompileSdkVersion = androidClass.methods.find { 
                                    it.name == "setCompileSdkVersion" || it.name == "setCompileSdk"
                                }
                                setCompileSdkVersion?.invoke(android, 34)
                            }
                        } else {
                            // 如果找不到 getter，直接尝试设置
                            val setCompileSdkVersion = androidClass.methods.find { 
                                it.name == "setCompileSdkVersion" || it.name == "setCompileSdk"
                            }
                            setCompileSdkVersion?.invoke(android, 34)
                        }
                    } catch (e: Exception) {
                        // 忽略错误，让 Flutter 插件加载器处理
                    }
                }
            } catch (e: Exception) {
                // 忽略配置错误，继续构建
            }
        }
    }
}

tasks.register<Delete>("clean") {
    delete(rootProject.layout.buildDirectory)
}
