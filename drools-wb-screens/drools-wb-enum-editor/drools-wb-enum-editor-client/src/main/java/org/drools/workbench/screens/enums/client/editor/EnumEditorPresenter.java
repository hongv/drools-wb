/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.drools.workbench.screens.enums.client.editor;

import java.util.List;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import com.google.gwt.user.client.ui.IsWidget;
import org.drools.workbench.screens.enums.client.type.EnumResourceType;
import org.drools.workbench.screens.enums.model.EnumModelContent;
import org.drools.workbench.screens.enums.service.EnumService;
import org.guvnor.common.services.shared.validation.model.ValidationMessage;
import org.jboss.errai.common.client.api.Caller;
import org.jboss.errai.common.client.api.RemoteCallback;
import org.kie.workbench.common.widgets.client.popups.validation.ValidationPopup;
import org.kie.workbench.common.widgets.client.resources.i18n.CommonConstants;
import org.kie.workbench.common.widgets.metadata.client.KieEditor;
import org.uberfire.backend.vfs.ObservablePath;
import org.uberfire.client.annotations.WorkbenchEditor;
import org.uberfire.client.annotations.WorkbenchMenu;
import org.uberfire.client.annotations.WorkbenchPartTitle;
import org.uberfire.client.annotations.WorkbenchPartTitleDecoration;
import org.uberfire.client.annotations.WorkbenchPartView;
import org.uberfire.ext.widgets.common.client.callbacks.CommandErrorCallback;
import org.uberfire.ext.widgets.common.client.callbacks.HasBusyIndicatorDefaultErrorCallback;
import org.uberfire.lifecycle.OnClose;
import org.uberfire.lifecycle.OnMayClose;
import org.uberfire.lifecycle.OnStartup;
import org.uberfire.mvp.Command;
import org.uberfire.mvp.PlaceRequest;
import org.uberfire.workbench.events.NotificationEvent;
import org.uberfire.workbench.model.menu.Menus;

/**
 * Enum Editor Presenter
 */
@Dependent
@WorkbenchEditor(identifier = "EnumEditor", supportedTypes = {EnumResourceType.class})
public class EnumEditorPresenter
        extends KieEditor {

    private EnumEditorView view;

    private Caller<EnumService> enumService;

    private EnumResourceType type;

    public EnumEditorPresenter() {
        //Zero-parameter constructor for CDI proxies
    }

    @Inject
    public EnumEditorPresenter(final EnumEditorView baseView,
                               final Caller<EnumService> enumService,
                               final EnumResourceType type) {
        super(baseView);
        this.view = baseView;
        this.enumService = enumService;
        this.type = type;
    }

    @OnStartup
    public void onStartup(final ObservablePath path,
                          final PlaceRequest place) {
        super.init(path,
                   place,
                   type);
    }

    protected void loadContent() {
        view.showLoading();
        enumService.call(getModelSuccessCallback(),
                         getNoSuchFileExceptionErrorCallback()).loadContent(versionRecordManager.getCurrentPath());
    }

    private RemoteCallback<EnumModelContent> getModelSuccessCallback() {
        return new RemoteCallback<EnumModelContent>() {

            @Override
            public void callback(final EnumModelContent content) {
                //Path is set to null when the Editor is closed (which can happen before async calls complete).
                if (versionRecordManager.getCurrentPath() == null) {
                    return;
                }

                resetEditorPages(content.getOverview());
                addSourcePage();

                final List<EnumRow> enumDefinitions = EnumParser.fromString(content.getModel().getEnumDefinitions());
                view.setContent(enumDefinitions);
                view.hideBusyIndicator();

                createOriginalHash(enumDefinitions);
            }
        };
    }

    @Override
    protected void onValidate(final Command callFinished) {
        enumService.call(new RemoteCallback<List<ValidationMessage>>() {
            @Override
            public void callback(final List<ValidationMessage> results) {
                if (results == null || results.isEmpty()) {
                    notification.fire(new NotificationEvent(CommonConstants.INSTANCE.ItemValidatedSuccessfully(),
                                                            NotificationEvent.NotificationType.SUCCESS));
                } else {
                    ValidationPopup.showMessages(results);
                }
                callFinished.execute();
            }
        }, new CommandErrorCallback(callFinished)).validate(versionRecordManager.getCurrentPath(),
                                                            EnumParser.toString(view.getContent()));
    }

    @Override
    protected void save(final String commitMessage) {
        final List<EnumRow> content = view.getContent();
        enumService.call(getSaveSuccessCallback(content.hashCode()),
                         new HasBusyIndicatorDefaultErrorCallback(view)).save(versionRecordManager.getCurrentPath(),
                                                                              EnumParser.toString(content),
                                                                              metadata,
                                                                              commitMessage);
    }

    @Override
    public void onSourceTabSelected() {
        updateSource(EnumParser.toString(view.getContent()));
    }

    @OnClose
    public void onClose() {
        this.versionRecordManager.clear();
    }

    @OnMayClose
    public boolean mayClose() {
        return super.mayClose(view.getContent());
    }

    @WorkbenchPartTitle
    public String getTitleText() {
        return super.getTitleText();
    }

    @WorkbenchPartTitleDecoration
    public IsWidget getTitle() {
        return super.getTitle();
    }

    @WorkbenchPartView
    public IsWidget getWidget() {
        return super.getWidget();
    }

    @WorkbenchMenu
    public Menus getMenus() {
        return menus;
    }
}
