import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.AppExtension

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
                val android = project.extensions.findByName("android")
                when (android) {
                    is LibraryExtension -> {
                        if (android.compileSdk == null) {
                            android.compileSdk = 34
                        }
                    }
                    is AppExtension -> {
                        if (android.compileSdk == null) {
                            android.compileSdk = 34
                        }
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
