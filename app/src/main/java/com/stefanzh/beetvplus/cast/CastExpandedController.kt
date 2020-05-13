package com.stefanzh.beetvplus.cast

import android.view.Menu
import com.google.android.gms.cast.framework.CastButtonFactory
import com.google.android.gms.cast.framework.media.widget.ExpandedControllerActivity
import com.stefanzh.beetvplus.R

class CastExpandedController : ExpandedControllerActivity() {

    /**
     * We need to populate the Cast button in the Expanded Controller
     * https://developers.google.com/cast/docs/design_checklist/sender#sender-expanded-controller
     */
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val result = super.onCreateOptionsMenu(menu)
        menuInflater.inflate(R.menu.expanded_controller, menu)
        CastButtonFactory.setUpMediaRouteButton(this, menu, R.id.media_route_menu_item)
        return result
    }
}
