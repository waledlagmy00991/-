package com.example

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast

class AlMinshawiAdminReceiver : DeviceAdminReceiver() {
    override fun onEnabled(context: Context, intent: Intent) {
        Toast.makeText(context, "تم تفعيل حماية النظام الفائقة", Toast.LENGTH_LONG).show()
    }

    override fun onDisabled(context: Context, intent: Intent) {
        Toast.makeText(context, "تم إيقاف حماية النظام الفائقة", Toast.LENGTH_LONG).show()
    }

    override fun onDisableRequested(context: Context, intent: Intent): CharSequence? {
        return "تنبيه هام للغاية: إلغاء نشاط الحماية الكلية يتيح حذف البرنامج يدويًا وقد يؤدي إلى تفويت الصلوات!"
    }
}
