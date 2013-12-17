/*
 * Copyright (C) 2012 eXo Platform SAS.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.gatein.portal.markdown;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URL;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;

import javax.xml.namespace.QName;

import juzu.Response;
import org.gatein.common.io.IOTools;
import org.gatein.portal.content.RenderTask;
import org.gatein.portal.content.Result;
import org.gatein.portal.content.WindowContent;
import org.gatein.portal.content.WindowContentContext;
import org.tautua.markdownpapers.parser.ParseException;
import org.w3c.dom.Element;

/**
 * @author Julien Viet
 */
class MarkdownContent extends WindowContent<Markdown> {

    private static final String SAMPLE;

    static {
        String s = "";
        InputStream in = MarkdownContent.class.getResourceAsStream("sample.md");
        if (in != null) {
            try {
                s = new String(IOTools.getBytes(in));
            } catch (IOException ignore) {
                // Should not happen...
            }
        }
        SAMPLE = s;
    }

    /** . */
    private final String id;

    /** . */
    private String windowState;

    /** . */
    private String mode;

    MarkdownContent(String id, String windowState, String mode) {
        this.id = id;
        this.windowState = windowState;
        this.mode = mode;
    }

    @Override
    public RenderTask createRender(final WindowContentContext<Markdown> window) {
        return new RenderTask() {
            @Override
            public Result.View execute(Locale locale) {
                if ("edit".equals(mode)) {
                    String saveURL = window.createActionURL(Collections.<String, String[]>emptyMap(), null, null);
                    String cancelURL = window.createRenderURL(new MarkdownContent(id, null, "view"), Collections.<String, String[]>emptyMap());
                    String markup = "<form class=\"form-horizontal\" action=\"" + saveURL + "\" method=\"POST\">" +
                            "<div class=\"control-group\">" +
                            "<label class=\"control-label\" for=\"markdown\">Markdown</label>" +
                            "<div class=\"controls\">" +
                            "<textarea id=\"markdown\" class=\"span8\" rows=\"20\" name=\"content\">" + (window.getState() != null ? window.getState().value : SAMPLE) + "</textarea>" +
                            "</div>" +
                            "</div>" +
                            "<div class=\"control-group\">" +
                            "<div class=\"controls controls-row\">" +
                            "<button type=\"submit\" class=\"btn btn-primary span2\">Save</button>" +
                            "<a href=\"" + cancelURL + "\" class=\"btn span2\">Cancel</a>" +
                            "</div>" +
                            "</div>" +
                            "</form>";
                    return new Result.Fragment(
                            Collections.<Map.Entry<String, String>>emptyList(),
                            Collections.<Element>emptyList(),
                            "Edit Markdown Content",
                            markup);
                } else {
                    String header;
                    if ("preview".equals(mode)) {
                        header = "";
                    } else {
                        MarkdownContent edit = new MarkdownContent(id, null, "edit");
                        String editURL = window.createRenderURL(edit, Collections.<String, String[]>emptyMap());
                        header = "<a href=\"" + editURL + "\">edit</a><br/>\n";
                    }
                    String markup;
                    try {
                        Markdown md = window.getState();
                        if (md != null) {
                            markup = render(md.value);
                        } else {
                            markup = render(SAMPLE);
                        }
                    } catch (ParseException e) {
                        return new Result.Error(false, e);
                    }
                    return new Result.Fragment(
                            Collections.<Map.Entry<String, String>>emptyList(),
                            Collections.<Element>emptyList(),
                            "Mardown Content",
                            header + markup);
                }
            }
        };
    }

    private String render(String md) throws ParseException {
        StringWriter buffer = new StringWriter();
        new org.tautua.markdownpapers.Markdown().transform(new StringReader(md), buffer);
        return buffer.toString();
    }

    @Override
    public Result.Action processAction(WindowContentContext<Markdown> window, String windowState, String mode, Map<String, String[]> interactionState) {
        String[] content = interactionState.get("content");
        Markdown md;
        if (content == null) {
            md = null;
        } else {
            md = new Markdown(content[0]);
        }
        return new Result.Update<Markdown>(
                "",
                null,
                "view",
                null,
                md
        );
    }

    @Override
    public Response serveResource(WindowContentContext<Markdown> window, String id, Map<String, String[]> resourceState) {
        throw new UnsupportedOperationException("todo");
    }

    @Override
    public String resolveTitle(Locale locale) {
        throw new UnsupportedOperationException("todo");
    }

    @Override
    public String getParameters() {
        return null;
    }

    @Override
    public void setParameters(String s) {
        // No navigational state supported
    }

    @Override
    public boolean isSupportedWindowState(String ws) {
        return true;
    }

    @Override
    public String getWindowState() {
        return windowState;
    }

    @Override
    public void setWindowState(String ws) {
        this.windowState = ws;
    }

    @Override
    public boolean isSupportedMode(String mode) {
        return true;
    }

    @Override
    public String getMode() {
        return mode;
    }

    @Override
    public void setMode(String m) {
        this.mode = m;
    }

    @Override
    public Map<String, String[]> computePublicParameters(Map<QName, String[]> parameters) {
        return Collections.emptyMap();
    }

    @Override
    public Iterable<Map.Entry<QName, String[]>> getPublicParametersChanges(Map<String, String[]> changes) {
        return Collections.emptyList();
    }

    @Override
    public WindowContent<Markdown> copy() {
        return new MarkdownContent(id, windowState, mode);
    }
}