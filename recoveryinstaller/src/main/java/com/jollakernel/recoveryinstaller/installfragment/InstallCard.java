/*
 * This file is part of MultiROM Manager.
 *
 * MultiROM Manager is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MultiROM Manager is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MultiROM Manager. If not, see <http://www.gnu.org/licenses/>.
 */

package com.jollakernel.recoveryinstaller.installfragment;

import android.content.Context;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.Spinner;

import com.fima.cardsui.objects.Card;
import com.jollakernel.recoveryinstaller.MainActivity;
import com.jollakernel.recoveryinstaller.Manifest;
import com.jollakernel.recoveryinstaller.R;
import com.jollakernel.recoveryinstaller.Recovery;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;
import java.util.Iterator;
import java.util.Map;

public class InstallCard extends Card implements CompoundButton.OnCheckedChangeListener, View.OnClickListener {

    public InstallCard(Bundle savedState, Manifest manifest, boolean forceRecovery, StartInstallListener listener) {
        super();
        m_manifest = manifest;
        m_listener = listener;
        m_forceRecovery = forceRecovery;
        m_savedState = savedState;
    }

    @Override
    public void saveInstanceState(Bundle outState) {
        super.saveInstanceState(outState);

        if(m_view == null)
            return;

        CheckBox b = (CheckBox)m_view.findViewById(R.id.install_recovery);
        outState.putBoolean("install_recovery", b.isChecked());
    }

    @Override
    public View getCardContent(Context context) {
        m_view = LayoutInflater.from(context).inflate(R.layout.install_card, null);

        Resources res = m_view.getResources();

        CheckBox b = (CheckBox)m_view.findViewById(R.id.install_recovery);
        if(m_manifest.getRecoveryFile() != null) {
            final Date rec_date = m_manifest.getRecoveryVersion();
            final String recovery_ver = Recovery.DISPLAY_FMT.format(rec_date);
            b.setText(res.getString(R.string.install_recovery, recovery_ver));
            b.setChecked(m_manifest.hasRecoveryUpdate());
            b.setOnCheckedChangeListener(this);

            // Force user to install recovery if not yet installed - it is needed to flash ZIPs
            if(m_manifest.hasRecoveryUpdate() && m_forceRecovery) {
                b.append(Html.fromHtml(res.getString(R.string.required)));
                b.setClickable(false);
            }
        } else {
            b.setChecked(false);
            b.setVisibility(View.GONE);
        }

        Button install_btn = (Button)m_view.findViewById(R.id.install_btn);
        install_btn.setOnClickListener(this);

        ImageButton changelog_btn = (ImageButton)m_view.findViewById(R.id.changelog_btn);
        if(m_manifest.getChangelogs() == null || m_manifest.getChangelogs().length == 0)
            changelog_btn.setVisibility(View.GONE);
        else
            changelog_btn.setOnClickListener(this);

        if(m_savedState != null)
            restoreInstanceState();

        enableInstallBtn();
        return m_view;
    }

    private void restoreInstanceState() {
        CheckBox b = (CheckBox)m_view.findViewById(R.id.install_recovery);
        b.setChecked(m_savedState.getBoolean("install_recovery", b.isChecked()));

        m_savedState = null;
    }

    @Override
    public void onCheckedChanged(CompoundButton btn, boolean checked) {
        enableInstallBtn();
    }

    private void enableInstallBtn() {
        Button install_btn = (Button)m_view.findViewById(R.id.install_btn);
        if(((CheckBox)m_view.findViewById(R.id.install_recovery)).isChecked()) {
            install_btn.setEnabled(true);
            return;
        }
        install_btn.setEnabled(false);
    }

    @Override
    public void onClick(View view) {
        switch(view.getId()) {
            case R.id.install_btn:
            {
                Bundle extras = new Bundle();

                Bundle bundle = new Bundle();
                bundle.putString("installation_type", "multirom");

                CheckBox b = (CheckBox)m_view.findViewById(R.id.install_recovery);
                bundle.putBoolean("install_recovery", b.isChecked());

                extras.putBundle("installation_info", bundle);

                m_listener.startActivity(extras, MainActivity.ACT_INSTALL_MULTIROM, InstallActivity.class);
                break;
            }
            case R.id.changelog_btn:
            {
                Changelog[] logs = m_manifest.getChangelogs();
                String[] names = new String[logs.length];
                String[] urls = new String[logs.length];

                for(int i = 0; i < logs.length; ++i) {
                    names[i] = logs[i].name;
                    urls[i] = logs[i].url;
                }

                Bundle b = new Bundle();
                b.putStringArray("changelog_names", names);
                b.putStringArray("changelog_urls", urls);

                m_listener.startActivity(b, MainActivity.ACT_CHANGELOG, ChangelogActivity.class);
                break;
            }
        }
    }

    private int getDefaultKernel() {
        int res = 0;
        Iterator<Map.Entry<String, Manifest.InstallationFile>> itr = m_manifest.getKernels().entrySet().iterator();
        for(int i = 0; itr.hasNext(); ++i) {
            JSONObject extra = itr.next().getValue().extra;
            if(extra == null)
                continue;
            try {
                if(extra.has("display") && Build.DISPLAY.indexOf(extra.getString("display")) == -1)
                    continue;

                if(extra.has("releases")) {
                    JSONArray r = extra.getJSONArray("releases");
                    boolean found = false;
                    for(int x = 0; x < r.length(); ++x) {
                        if(r.getString(x).equals(Build.VERSION.RELEASE)) {
                            found = true;
                            break;
                        }
                    }
                    if(!found)
                        continue;
                }
                res = i;
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return res;
    }

    private Manifest m_manifest;
    private View m_view;
    private StartInstallListener m_listener;
    private boolean m_forceRecovery;
    private Bundle m_savedState;
}

