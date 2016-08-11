// Copyright (C) 2016 BarD Software
package biz.ganttproject.storage.cloud;

import biz.ganttproject.storage.StorageDialogBuilder;
import javafx.collections.ObservableList;
import javafx.concurrent.Worker;
import javafx.event.ActionEvent;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TreeItem;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import net.sourceforge.ganttproject.GPLogger;
import net.sourceforge.ganttproject.document.Document;
import net.sourceforge.ganttproject.document.webdav.HttpDocument;
import net.sourceforge.ganttproject.document.webdav.WebDavResource;
import net.sourceforge.ganttproject.document.webdav.WebDavServerDescriptor;
import org.controlsfx.control.BreadCrumbBar;
import org.controlsfx.control.MaskerPane;

import java.io.IOException;
import java.util.function.Consumer;

/**
 * @author dbarashev@bardsoftware.com
 */
public class WebdavStorage implements StorageDialogBuilder.Ui {
  private final Consumer<Document> myOpenDocument;
  private final Consumer<Document> myReplaceDocument;
  private final StorageDialogBuilder.DialogUi myDialogUi;
  private WebdavLoadService myLoadService;
  private WebDavServerDescriptor myServer;
  private BreadCrumbBar<BreadCrumbNode> myBreadcrumbs;
  private ListView<WebDavResource> myFilesTable;
  private Consumer<TreeItem<BreadCrumbNode>> myOnSelectCrumb;
  private WebDavResource myCurrentFolder;

  public WebdavStorage(Consumer<Document> openDocument, Consumer<Document> replaceDocument, StorageDialogBuilder.DialogUi dialogUi) {
    myOpenDocument = openDocument;
    myReplaceDocument = replaceDocument;
    myDialogUi = dialogUi;
  }

  void setServer(WebDavServerDescriptor webdavServer) {
    myLoadService = new WebdavLoadService(webdavServer);
    myServer = webdavServer;
  }
  @Override
  public String getId() {
    return null;
  }

  static class BreadCrumbNode {
    private String path;
    private String label;
    BreadCrumbNode(String path, String label) { this.path = path; this.label = label; }

    @Override
    public String toString() {
      return this.label;
    }
  }

  @Override
  public Pane createUi() {
    VBox rootPane = new VBox();
    rootPane.getStyleClass().add("pane-service-contents");
    rootPane.setPrefWidth(400);

    VBox buttonPane = new VBox();

    buttonPane.getStyleClass().add("webdav-button-pane");
    ButtonBar buttonBar = new ButtonBar("L+");

    Button btnOpen = new Button("Open");
    ButtonBar.setButtonData(btnOpen, ButtonBar.ButtonData.LEFT);
    btnOpen.addEventHandler(ActionEvent.ACTION, event -> openResource());

    Button btnSave = new Button("Save");
    ButtonBar.setButtonData(btnSave, ButtonBar.ButtonData.LEFT);
    btnSave.addEventHandler(ActionEvent.ACTION, event -> {
      try {
        Document document = createDocument(myLoadService.createResource(myCurrentFolder, "Foo.gan"));
        myReplaceDocument.accept(document);
      } catch (IOException e) {
        e.printStackTrace();
      }
    });

    Button btnSaveAs = new Button("Save As");
    ButtonBar.setButtonData(btnSaveAs, ButtonBar.ButtonData.LEFT);

    buttonBar.getButtons().addAll(btnOpen, btnSave, btnSaveAs);
    buttonPane.getChildren().add(buttonBar);
    rootPane.getChildren().add(buttonPane);

    myBreadcrumbs = new BreadCrumbBar<>();
    myBreadcrumbs.getStyleClass().add("breadcrumb");

    rootPane.getChildren().add(myBreadcrumbs);

    myFilesTable = new ListView<>();
    myFilesTable.setCellFactory(param -> new ListCell<WebDavResource>() {
      @Override
      protected void updateItem(WebDavResource item, boolean empty) {
        if (item == null) {
          setText("");
        } else {
          super.updateItem(item, empty);
          if (empty) {
            setGraphic(null);
          } else {
            setText(item.getName());
          }
        }
      }
    });
    rootPane.getChildren().add(myFilesTable);
    StackPane stackPane = new StackPane();
    MaskerPane maskerPane = new MaskerPane();
    stackPane.getChildren().addAll(rootPane, maskerPane);

    TreeItem<BreadCrumbNode> rootItem = new TreeItem<>(new BreadCrumbNode("/", myServer.name));
    myOnSelectCrumb = selectedCrumb -> {
      selectedCrumb.getChildren().clear();
      loadFolder(selectedCrumb.getValue().path, maskerPane::setVisible, myFilesTable::setItems, myDialogUi);
    };
    myBreadcrumbs.setOnCrumbAction(value -> myOnSelectCrumb.accept(value.getSelectedCrumb()));
    myBreadcrumbs.setSelectedCrumb(rootItem);
    myOnSelectCrumb.accept(rootItem);

    myFilesTable.setOnMouseClicked(event -> {
      if (event.getClickCount() == 2) {
        openResource();
      }
    });

    return stackPane;
  }

  private void openResource() {
    try {
      WebDavResource selectedItem = myFilesTable.getSelectionModel().getSelectedItem();
      if (selectedItem.isCollection()) {
        BreadCrumbNode crumbNode = new BreadCrumbNode(selectedItem.getAbsolutePath(), selectedItem.getName());
        TreeItem<BreadCrumbNode> treeItem = new TreeItem<>(crumbNode);
        myBreadcrumbs.getSelectedCrumb().getChildren().add(treeItem);
        myBreadcrumbs.setSelectedCrumb(treeItem);
        myOnSelectCrumb.accept(treeItem);
      } else {
        myOpenDocument.accept(createDocument(selectedItem));
      }
    } catch (IOException | WebDavResource.WebDavException e) {
      myDialogUi.error(e);
    }

  }
  private void loadFolder(String path, Consumer<Boolean> showMaskPane, Consumer<ObservableList<WebDavResource>> setResult, StorageDialogBuilder.DialogUi dialogUi) {
    myLoadService.setPath(path);
    myCurrentFolder = myLoadService.createRootResource();
    myLoadService.setOnSucceeded((event) -> {
      Worker<ObservableList<WebDavResource>> source = event.getSource();
      setResult.accept(source.getValue());
      showMaskPane.accept(false);
    });
    myLoadService.setOnFailed((event) -> {
      showMaskPane.accept(false);
      dialogUi.error("WebdavService failed!");
    });
    myLoadService.setOnCancelled((event) -> {
      showMaskPane.accept(false);
      GPLogger.log("WebdavService cancelled!");
    });
    myLoadService.restart();
    showMaskPane.accept(true);
  }

  private Document createDocument(WebDavResource resource) throws IOException {
    return new HttpDocument(resource, myServer.getUsername(), myServer.getPassword(), HttpDocument.NO_LOCK);
  }
}
