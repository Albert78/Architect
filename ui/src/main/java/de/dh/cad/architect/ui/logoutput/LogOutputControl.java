/*******************************************************************************
 *     Architect - A free 2D/3D home and interior designer
 *     Copyright (C) 2021 - 2023  Daniel Höh
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>
 *******************************************************************************/
package de.dh.cad.architect.ui.logoutput;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Writer;

import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;

public class LogOutputControl extends BorderPane {
    protected LogOutputControl() {
//        FXMLLoader fxmlLoader = new FXMLLoader(LogOutputControl.class.getResource(FXML));
//        fxmlLoader.setRoot(this);
//        fxmlLoader.setController(this);
//        try {
//            fxmlLoader.load();
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
//        initialize();
        setCenter(new Label("Log Output Dummy"));
    }

    public static LogOutputControl create() {
        return new LogOutputControl();
    }

    protected void initialize() {
        // TODO
    }

    public Writer getLogOutputWriter() {
        return new BufferedWriter(new Writer() {
            @Override
            public void write(char[] cbuf, int off, int len) throws IOException {
                String out = new String(cbuf, off, len);
                // TODO: Write out to output
            }

            @Override
            public void flush() throws IOException {
                // Nothing to do
            }

            @Override
            public void close() throws IOException {
                // Nothing to do
            }
        });
    }
}
