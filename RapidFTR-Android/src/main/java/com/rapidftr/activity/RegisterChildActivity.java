package com.rapidftr.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.widget.*;
import com.rapidftr.R;
import com.rapidftr.adapter.FormSectionPagerAdapter;
import com.rapidftr.forms.FormSection;
import com.rapidftr.model.Child;
import com.rapidftr.repository.ChildRepository;
import com.rapidftr.utils.AsyncTaskWithMessage;
import lombok.Cleanup;
import org.json.JSONException;

import java.util.List;

public class RegisterChildActivity extends RapidFtrActivity implements AsyncTaskWithMessage.BackgroundWorker<Child, Boolean> {

    public static final int CLOSE_ACTIVITY = 999;
    protected List<FormSection> formSections;
    protected Child child;
    protected boolean editable = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null && savedInstanceState.containsKey("child_state")) {
            try {
                child = new Child(savedInstanceState.getString("child_state"));
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
        } else {
            tryPopulatingChildFromParcel();
        }
        initialize();
    }

    protected void tryPopulatingChildFromParcel() {
        try {
            if( getIntent().getParcelableExtra("child") != null)
                child = new Child(((Child) getIntent().getParcelableExtra("child")).getJsonString());
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("child_state", child.toString());
    }

    protected void initialize() {
        setContentView(R.layout.activity_register_child);
        initializeData();
        initializePager();
        initializeSpinner();
        initializeListeners();
        setLabel(R.string.save);
        setTitle();
    }

    protected void setLabel(int label) {
        ((Button) findViewById(R.id.submit)).setText(label);
    }

    protected void setTitle() {
        try {
            String title = child == null || child.getShortId() == null ? getString(R.string.new_registration) : child.getShortId();
            ((TextView) findViewById(R.id.title)).setText(title);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    protected void initializeData() {
        if (child == null) child = new Child();
        this.formSections = getContext().getFormSections();
    }

    protected void initializeListeners() {
        findViewById(R.id.submit).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                saveChild();
            }
        });
    }

    protected Spinner getSpinner() {
        return ((Spinner) findViewById(R.id.spinner));
    }

    protected ViewPager getPager() {
        return (ViewPager) findViewById(R.id.pager);
    }

    protected void initializePager() {
        getPager().setAdapter(new FormSectionPagerAdapter(formSections, child, editable));
        getPager().setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                getSpinner().setSelection(position);
            }

        });
    }

    protected void initializeSpinner() {
        getSpinner().setAdapter(new ArrayAdapter<FormSection>(this, android.R.layout.simple_spinner_item, formSections));
        getSpinner().setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                getPager().setCurrentItem(position);
            }

            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    @Override
    public Boolean doInBackground(Child... children) throws JSONException {
        child.setOwner(getContext().getUserName());
        child.generateUniqueId();

        @Cleanup ChildRepository repository = getInjector().getInstance(ChildRepository.class);
        repository.createOrUpdate(child);
        return true;
    }

    @Override
    public void onSuccess() throws Exception {
        Intent intent = new Intent(RegisterChildActivity.this, ViewChildActivity.class);
        intent.putExtra("id", child.getUniqueId());
        child.put("saved", true);
        finish();
        startActivity(intent);
    }

    protected void saveChild() {
        if (!child.isValid()) {
            makeToast(R.string.save_child_invalid);
        } else {
            AsyncTaskWithMessage<Child, Boolean, RegisterChildActivity> task =
                    new AsyncTaskWithMessage<Child, Boolean, RegisterChildActivity>(this, R.string.save_child_progress, R.string.save_child_success, R.string.save_child_failure);
            task.execute(child);
        }
    }

}
