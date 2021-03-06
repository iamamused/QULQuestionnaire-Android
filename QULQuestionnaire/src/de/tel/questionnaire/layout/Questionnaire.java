/*
 * QUe
 * 
 * Copyright (c) 2014 Quality and Usability Lab,
 * Telekom Innvation Laboratories, TU Berlin. All rights reserved.
 * https://github.com/QULab/QUe-Android
 * 
 * This file is part of QUe.
 * 
 * QUe is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * QUe is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with QUe. If not, see <http://www.gnu.org/licenses/>.
 */
package de.tel.questionnaire.layout;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import de.tel.questionnaire.entities.BasisQuestionEntity;
import de.tel.questionnaire.util.AnswerLogging;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 * @author Christopher Zell <zelldon91@googlemail.com>
 */
public class Questionnaire {
  
  public static final String PREF_KEY_QUESTIONNAIRE_FINISHED = "questionnaire_complete";
  public static final Integer BTN_NEXT_ID = 0xFF231;
  private final Context context;
  private final Map<String, Class<? extends QuestionLayout>> layoutBuilders;
  private final AnswerLogging logging;
  private boolean finished = false;

  public Questionnaire(AnswerLogging logging, Context context, Map<String, Class<? extends QuestionLayout>> layoutBuilders) {
    this.context = context;
    this.layoutBuilders = layoutBuilders;
    this.logging = logging;
  }

  public View createQuestion(JSONArray array) throws JSONException, InstantiationException {
    final LinearLayout ll = new LinearLayout(context);
    ll.setOrientation(LinearLayout.VERTICAL);
    if (array.length() == 0) {
      return ll;
    }

    return createQuestion(ll, array, 0);
  }

  public boolean isFinished() {
    return finished;
  }

  private View createQuestion(final LinearLayout ll, final JSONArray array, final int step) throws JSONException, InstantiationException {
    if (array.length() == step) { //anchor
      //finish activity
      finished = true;
      SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
      preferences.edit().putBoolean(PREF_KEY_QUESTIONNAIRE_FINISHED, true).apply();
      ((Activity) context).finish();
      return ll;
    }

    JSONObject json = (JSONObject) array.getJSONObject(step);
    QuestionLayout qlayoutBuilder = createQuestionLayoutInstance(json.getString(QuestionLayout.JSON_KEY_TYPE));
    
    if (qlayoutBuilder == null) {
      qlayoutBuilder = createDefaultQuestionLayout();
    }
    BasisQuestionEntity questionEntity = qlayoutBuilder.getQuestion(json);
    addTextView(ll, questionEntity.getQuestion(), 18);
    addTextView(ll, questionEntity.getInstruction(), 12);
    qlayoutBuilder.createQuestionLayout(ll, questionEntity);
    addButton(qlayoutBuilder, questionEntity, ll, array, step);
    return ll;
  }
  
  private QuestionLayout createDefaultQuestionLayout() {
    return new QuestionLayout(context, logging) {
        @Override
        public LinearLayout addQuestionLayout(LinearLayout ll,
                BasisQuestionEntity basis,
                final Button next) {
          next.setVisibility(View.VISIBLE);
          return ll;
        }

        @Override
        public String getLastGivenAnswer() {
          return "";
        }

        @Override
        public String getType() {
          return "";
        }
      };
  }
  
  private QuestionLayout createQuestionLayoutInstance(String type) throws InstantiationException {
    QuestionLayout qlayoutBuilder = null;
    Class<? extends QuestionLayout> layoutClass = layoutBuilders.get(type);
    if (layoutClass != null) {
      try {
        Constructor ctor = layoutClass.getConstructor(Context.class, AnswerLogging.class);
        qlayoutBuilder = (QuestionLayout) ctor.newInstance(context, logging);
      } catch (NoSuchMethodException ex) {
        Logger.getLogger(Questionnaire.class.getName()).log(Level.SEVERE, null, ex);
      } catch (IllegalAccessException ex) {
        Logger.getLogger(Questionnaire.class.getName()).log(Level.SEVERE, null, ex);
      } catch (IllegalArgumentException ex) {
        Logger.getLogger(Questionnaire.class.getName()).log(Level.SEVERE, null, ex);
      } catch (InvocationTargetException ex) {
        Logger.getLogger(Questionnaire.class.getName()).log(Level.SEVERE, null, ex);
      }
    }
    return qlayoutBuilder;
  }

  private void addButton(final QuestionLayout layoutBuilder,
                         final BasisQuestionEntity entity,
                         final LinearLayout ll,
                         final JSONArray array,
                         final int step) {
    Button btn = (Button) ll.findViewById(BTN_NEXT_ID);
    btn.setOnClickListener(new View.OnClickListener() {
      public void onClick(View arg0) {
        logging.addAnswer(entity.getKey(), layoutBuilder.getLastGivenAnswer());
        ll.removeAllViews();
        try {
          createQuestion(ll, array, step + 1); //recursion
        } catch (JSONException ex) {
          Log.e(Questionnaire.class.getName(), ex.getMessage(), ex);
        } catch (InstantiationException ex) {
          Logger.getLogger(Questionnaire.class.getName()).log(Level.SEVERE, null, ex);
        }
      }
    });
  }

  private void addTextView(LinearLayout ll, String content, int textSize) {
    TextView view = new TextView(context);
    view.setText(content);
    view.setTextSize(textSize);
    ll.addView(view);
  }
}
