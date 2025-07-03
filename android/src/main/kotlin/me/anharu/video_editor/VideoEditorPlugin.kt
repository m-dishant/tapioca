package me.anharu.video_editor

import android.Manifest
import android.app.Activity
import androidx.annotation.NonNull
import androidx.core.app.ActivityCompat
import com.daasuu.mp4compose.composer.Mp4Composer
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result

class VideoEditorPlugin : FlutterPlugin, MethodCallHandler, EventChannel.StreamHandler, ActivityAware {
    private var activity: Activity? = null
    private var methodChannel: MethodChannel? = null
    private var eventChannel: EventChannel? = null
    private val myPermissionCode = 34264
    private var eventSink: EventChannel.EventSink? = null

    override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        methodChannel = MethodChannel(flutterPluginBinding.binaryMessenger, "video_editor")
        eventChannel = EventChannel(flutterPluginBinding.binaryMessenger, "video_editor_progress")
        methodChannel?.setMethodCallHandler(this)
        eventChannel?.setStreamHandler(this)
    }

    override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
        methodChannel?.setMethodCallHandler(null)
        eventChannel?.setStreamHandler(null)
        methodChannel = null
        eventChannel = null
    }

    override fun onMethodCall(@NonNull call: MethodCall, @NonNull result: Result) {
        if (call.method == "getPlatformVersion") {
            result.success("Android ${android.os.Build.VERSION.RELEASE}")
        } else if (call.method == "writeVideofile") {
            val currentActivity = activity
            if (currentActivity == null) {
                result.error("activity_not_found", "Activity is not found.", null)
                return
            }

            if (eventSink == null) {
                println("event_sink_null Warning: eventSink is null. Make sure Flutter is listening to the event channel")
            }

            checkPermission(currentActivity)

            val srcFilePath: String = call.argument("srcFilePath") ?: run {
                result.error("src_file_path_not_found", "The source file path is not found.", null)
                return
            }

            val destFilePath: String = call.argument("destFilePath") ?: run {
                result.error("dest_file_path_not_found", "The destination file path is not found.", null)
                return
            }

            val processing: HashMap<String, HashMap<String, Any>> = call.argument("processing") ?: run {
                result.error("processing_data_not_found", "Processing data is not found.", null)
                return
            }

            val generator = VideoGeneratorService(Mp4Composer(srcFilePath, destFilePath))
            generator.writeVideofile(processing, result, currentActivity, eventSink)
        } else {
            result.notImplemented()
        }
    }

    // EventChannel.StreamHandler
    override fun onListen(arguments: Any?, events: EventChannel.EventSink) {
        eventSink = events
    }

    override fun onCancel(arguments: Any?) {
        eventSink = null
    }

    // ActivityAware
    override fun onAttachedToActivity(binding: ActivityPluginBinding) {
        activity = binding.activity
    }

    override fun onDetachedFromActivity() {
        activity = null
    }

    override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
        onAttachedToActivity(binding)
    }

    override fun onDetachedFromActivityForConfigChanges() {
        onDetachedFromActivity()
    }

    private fun checkPermission(activity: Activity) {
        ActivityCompat.requestPermissions(
            activity,
            arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ),
            myPermissionCode
        )
    }
}
