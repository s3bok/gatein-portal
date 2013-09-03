/*
 * Copyright (C) 2011 eXo Platform SAS.
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

package org.exoplatform.portal.mop.navigation;

import java.util.Iterator;

import org.exoplatform.portal.mop.AbstractMopServiceTest;
import org.gatein.portal.mop.hierarchy.HierarchyError;
import org.gatein.portal.mop.hierarchy.HierarchyException;
import org.gatein.portal.mop.hierarchy.NodeData;
import org.gatein.portal.mop.navigation.NavigationNode;
import org.gatein.portal.mop.site.SiteKey;
import org.gatein.portal.mop.hierarchy.NodeChange;
import org.gatein.portal.mop.hierarchy.NodeChangeQueue;
import org.gatein.portal.mop.hierarchy.NodeContext;
import org.gatein.portal.mop.hierarchy.Scope;
import org.gatein.portal.mop.navigation.NavigationContext;
import org.gatein.portal.mop.navigation.NavigationServiceException;
import org.gatein.portal.mop.navigation.NodeState;
import org.gatein.portal.mop.site.SiteType;

/**
 * @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a>
 */
public class TestNavigationServiceUpdate extends AbstractMopServiceTest {

    public void testNoop() throws Exception {
        NodeData node = createNavigation(createSite(SiteType.PORTAL, "update_no_op"));
        createNodeChild(node, "a", "b", "c", "d");

        //
        sync(true);

        //
        NavigationContext navigation = getNavigationService().loadNavigation(SiteKey.portal("update_no_op"));
        NodeContext<NavigationNode, NodeState> root = getNavigationService().loadNode(NavigationNode.MODEL, navigation, Scope.ALL, null);
        Iterator<NodeChange<NavigationNode, NodeState>> it = root.getNode().update(getNavigationService(), null);
        assertFalse(it.hasNext());
    }

    public void testHasChanges() throws Exception {
        createNavigation(createSite(SiteType.PORTAL, "update_cannot_save"));

        //
        sync(true);

        //
        NavigationContext navigation = getNavigationService().loadNavigation(SiteKey.portal("update_cannot_save"));
        NavigationNode root = getNavigationService().loadNode(NavigationNode.MODEL, navigation, Scope.ALL, null).getNode();

        //
        assertFalse(root.getContext().hasChanges());
        root.addChild("foo");
        assertTrue(root.getContext().hasChanges());

        //
        try {
            root.update(getNavigationService(), null);
        } catch (IllegalArgumentException expected) {
        }

        //
        assertTrue(root.getContext().hasChanges());
        getNavigationService().saveNode(root.getContext(), null);
        assertFalse(root.getContext().hasChanges());

        //
        Iterator<NodeChange<NavigationNode, NodeState>> it = root.update(getNavigationService(), null);
        assertFalse(it.hasNext());
    }

    public void testAddFirst() throws Exception {
        createNavigation(createSite(SiteType.PORTAL, "update_add_first"));

        //
        sync(true);

        //
        NavigationContext navigation = getNavigationService().loadNavigation(SiteKey.portal("update_add_first"));
        NodeContext<NavigationNode, NodeState> root1 = getNavigationService().loadNode(NavigationNode.MODEL, navigation, Scope.ALL, null);
        assertEquals(0, root1.getNodeSize());
        NavigationNode root2 = getNavigationService().loadNode(NavigationNode.MODEL, navigation, Scope.ALL, null).getNode();
        root2.addChild("a");
        getNavigationService().saveNode(root2.getContext(), null);

        //
        sync(true);

        //
        root1.getNode().update(getNavigationService(), null);
        assertEquals(1, root1.getNodeSize());
        NavigationNode a = root1.getNode(0);
        assertEquals("a", a.getName());

    }

    public void testAddSecond() throws Exception {
        NodeData node = createNavigation(createSite(SiteType.PORTAL, "update_add_second"));
        createNodeChild(node, "a");

        //
        sync(true);

        //
        NavigationContext navigation = getNavigationService().loadNavigation(SiteKey.portal("update_add_second"));
        NavigationNode root1 = getNavigationService().loadNode(NavigationNode.MODEL, navigation, Scope.ALL, null).getNode();
        NavigationNode a = root1.getChild("a");
        assertEquals(1, root1.getSize());
        NavigationNode root2 = getNavigationService().loadNode(NavigationNode.MODEL, navigation, Scope.ALL, null).getNode();
        root2.addChild("b");
        getNavigationService().saveNode(root2.getContext(), null);

        //
        sync(true);

        //
        Iterator<NodeChange<NavigationNode, NodeState>> changes = root1.update(getNavigationService(), null);
        NodeChange.Added<NavigationNode, NodeState> added = (NodeChange.Added<NavigationNode, NodeState>) changes.next();
        assertSame(root1, added.getParent());
        assertSame(root1.getChild("b"), added.getTarget());
        assertSame(a, added.getPrevious());
        assertFalse(changes.hasNext());
        assertEquals(2, root1.getSize());
        assertEquals("a", root1.getChild(0).getName());
        assertEquals("b", root1.getChild(1).getName());
    }

    public void testRemove() throws Exception {
        NodeData node = createNavigation(createSite(SiteType.PORTAL, "update_remove"));
        createNodeChild(node, "a");

        //
        sync(true);

        //
        NavigationContext navigation = getNavigationService().loadNavigation(SiteKey.portal("update_remove"));
        NodeContext<NavigationNode, NodeState> root1 = getNavigationService().loadNode(NavigationNode.MODEL, navigation, Scope.ALL, null);
        assertEquals(1, root1.getNodeSize());
        NavigationNode a = root1.getNode("a");
        NavigationNode root2 = getNavigationService().loadNode(NavigationNode.MODEL, navigation, Scope.ALL, null).getNode();
        root2.removeChild("a");
        getNavigationService().saveNode(root2.getContext(), null);

        //
        sync(true);

        //
        Iterator<NodeChange<NavigationNode, NodeState>> changes = root1.getNode().update(getNavigationService(), null);
        NodeChange.Removed<NavigationNode, NodeState> removed = (NodeChange.Removed<NavigationNode, NodeState>) changes.next();
        assertSame(root1.getNode(), removed.getParent());
        assertSame(a, removed.getTarget());
        assertFalse(changes.hasNext());
        assertEquals(0, root1.getNodeSize());
    }

    public void testMove() throws Exception {
        NodeData node = createNavigation(createSite(SiteType.PORTAL, "update_move"));
        createNodeChild(createNodeChild(node, "a", "c")[0], "b");

        //
        sync(true);

        //
        NavigationContext navigation = getNavigationService().loadNavigation(SiteKey.portal("update_move"));
        NodeContext<NavigationNode, NodeState> root1 = getNavigationService().loadNode(NavigationNode.MODEL, navigation, Scope.ALL, null);
        assertEquals(2, root1.getNodeSize());
        NavigationNode a = root1.getNode("a");
        NavigationNode b = a.getChild("b");
        NavigationNode c = root1.getNode("c");
        NavigationNode root2 = getNavigationService().loadNode(NavigationNode.MODEL, navigation, Scope.ALL, null).getNode();
        root2.getChild("c").addChild(root2.getChild("a").getChild("b"));
        getNavigationService().saveNode(root2.getContext(), null);

        //
        sync(true);

        //
        Iterator<NodeChange<NavigationNode, NodeState>> changes = root1.getNode().update(getNavigationService(), null);
        NodeChange.Moved<NavigationNode, NodeState> moved = (NodeChange.Moved<NavigationNode, NodeState>) changes.next();
        assertSame(a, moved.getFrom());
        assertSame(c, moved.getTo());
        assertSame(b, moved.getTarget());
        assertSame(null, moved.getPrevious());
        assertFalse(changes.hasNext());
        assertEquals(0, root1.getNode("a").getSize());
        assertEquals(1, root1.getNode("c").getSize());
    }

    public void testAddWithSameName() throws Exception {
        createNavigation(createSite(SiteType.PORTAL, "update_add_with_same_name"));

        //
        sync(true);

        //
        NavigationContext navigation = getNavigationService().loadNavigation(SiteKey.portal("update_add_with_same_name"));
        NavigationNode root1 = getNavigationService().loadNode(NavigationNode.MODEL, navigation, Scope.ALL, null).getNode();
        root1.addChild("a").addChild("b");
        root1.addChild("c");
        getNavigationService().saveNode(root1.getContext(), null);

        //
        sync(true);

        //
        root1 = getNavigationService().loadNode(NavigationNode.MODEL, navigation, Scope.ALL, null).getNode();
        NavigationNode a = root1.getChild("a");
        NavigationNode b = a.getChild("b");
        NavigationNode c = root1.getChild("c");

        //
        NavigationNode root2 = getNavigationService().loadNode(NavigationNode.MODEL, navigation, Scope.ALL, null).getNode();
        root2.getChild("c").addChild(root2.getChild("a").getChild("b"));
        NavigationNode b2 = root2.getChild("a").addChild("b");
        getNavigationService().saveNode(root2.getContext(), null);

        //
        Iterator<NodeChange<NavigationNode, NodeState>> changes = root1.update(getNavigationService(), null);
        NodeChange.Added<NavigationNode, NodeState> added = (NodeChange.Added<NavigationNode, NodeState>) changes.next();
        assertNull(added.getPrevious());
        assertSame(a, added.getParent());
        NodeChange.Moved<NavigationNode, NodeState> moved = (NodeChange.Moved<NavigationNode, NodeState>) changes.next();
        assertNull(moved.getPrevious());
        assertSame(a, moved.getFrom());
        assertSame(c, moved.getTo());
        assertSame(b, moved.getTarget());
        assertFalse(changes.hasNext());

        //
        assertSame(a, root1.getChild("a"));
        assertSame(c, root1.getChild("c"));
        assertSame(b, c.getChild("b"));
        assertEquals(b2.getId(), a.getChild("b").getId());
        assertSame(a.getChild("b"), added.getTarget());
    }

    public void testComplex() throws Exception {
        NodeData node = createNavigation(createSite(SiteType.PORTAL, "update_complex"));

        //
        sync(true);

        //
        NavigationContext navigation = getNavigationService().loadNavigation(SiteKey.portal("update_complex"));
        NavigationNode root1 = getNavigationService().loadNode(NavigationNode.MODEL, navigation, Scope.ALL, null).getNode();
        NavigationNode a1 = root1.addChild("a");
        a1.addChild("c");
        a1.addChild("d");
        a1.addChild("e");
        NavigationNode b1 = root1.addChild("b");
        b1.addChild("f");
        b1.addChild("g");
        b1.addChild("h");
        getNavigationService().saveNode(root1.getContext(), null);

        //
        sync(true);

        //
        root1 = getNavigationService().loadNode(NavigationNode.MODEL, navigation, Scope.ALL, null).getNode();
        a1 = root1.getChild("a");
        NavigationNode c1 = a1.getChild("c");
        NavigationNode d1 = a1.getChild("d");
        NavigationNode e1 = a1.getChild("e");
        b1 = root1.getChild("b");
        NavigationNode f1 = b1.getChild("f");
        NavigationNode g1 = b1.getChild("g");
        NavigationNode h1 = b1.getChild("h");

        //
        NavigationNode root2 = getNavigationService().loadNode(NavigationNode.MODEL, navigation, Scope.ALL, null).getNode();
        NavigationNode a2 = root2.getChild("a");
        a2.removeChild("e");
        NavigationNode b2 = root2.getChild("b");
        b2.addChild(2, a2.getChild("d"));
        a2.addChild(1, "d");
        b2.removeChild("g");
        getNavigationService().saveNode(root2.getContext(), null);

        //
        sync(true);

        //
        Iterator<NodeChange<NavigationNode, NodeState>> changes = root1.update(getNavigationService(), null);
        NodeChange.Added<NavigationNode, NodeState> added = (NodeChange.Added<NavigationNode, NodeState>) changes.next();
        assertSame(a1, added.getParent());
        assertEquals("d", added.getTarget().getName());
        assertSame(c1, added.getPrevious());
        NodeChange.Removed<NavigationNode, NodeState> removed1 = (NodeChange.Removed<NavigationNode, NodeState>) changes.next();
        assertSame(a1, removed1.getParent());
        assertSame(e1, removed1.getTarget());
        NodeChange.Moved<NavigationNode, NodeState> moved = (NodeChange.Moved<NavigationNode, NodeState>) changes.next();
        assertSame(a1, moved.getFrom());
        assertSame(b1, moved.getTo());
        assertSame(d1, moved.getTarget());
        assertSame(f1, moved.getPrevious());
        NodeChange.Removed<NavigationNode, NodeState> removed2 = (NodeChange.Removed<NavigationNode, NodeState>) changes.next();
        assertSame(b1, removed2.getParent());
        assertSame(g1, removed2.getTarget());
        assertFalse(changes.hasNext());

        //
        assertSame(a1, root1.getChild("a"));
        assertSame(b1, root1.getChild("b"));
        assertEquals(2, a1.getSize());
        assertSame(c1, a1.getChild(0));
        assertNotNull(a1.getChild(1));
        assertEquals("d", a1.getChild(1).getName());
        assertFalse(d1.getId().equals(a1.getChild(1).getId()));
        assertEquals(3, b1.getSize());
        assertSame(f1, b1.getChild(0));
        assertSame(d1, b1.getChild(1));
        assertSame(h1, b1.getChild(2));
    }

    public void testReplaceChild() throws NullPointerException, NavigationServiceException {
        NodeData node = createNavigation(createSite(SiteType.PORTAL, "update_replace_child"));
        createNodeChild(node, "foo");
        sync(true);

        //
        NavigationContext navigation = getNavigationService().loadNavigation(SiteKey.portal("update_replace_child"));
        NavigationNode root1 = getNavigationService().loadNode(NavigationNode.MODEL, navigation, Scope.CHILDREN, null).getNode();
        String foo1Id = root1.getChild("foo").getId();

        //
        NavigationNode root2 = getNavigationService().loadNode(NavigationNode.MODEL, navigation, Scope.CHILDREN, null).getNode();
        root2.removeChild("foo");
        NavigationNode foo = root2.addChild("foo");
        foo.setState(new NodeState.Builder().label("foo2").build());
        getNavigationService().saveNode(root2.getContext(), null);
        String foo2Id = foo.getId();
        sync(true);

        //
        Iterator<NodeChange<NavigationNode, NodeState>> changes = root1.update(getNavigationService(), null);
        NodeChange.Added<NavigationNode, NodeState> added = (NodeChange.Added<NavigationNode, NodeState>) changes.next();
        assertEquals(foo2Id, added.getTarget().getId());
        NodeChange.Removed<NavigationNode, NodeState> removed = (NodeChange.Removed<NavigationNode, NodeState>) changes.next();
        assertEquals(foo1Id, removed.getTarget().getId());
        assertFalse(changes.hasNext());

        //
        foo = root1.getChild("foo");
        assertEquals(foo2Id, foo.getId());
        assertEquals("foo2", root1.getChild("foo").getState().getLabel());
    }

    public void testRename() throws NullPointerException, NavigationServiceException {
        NodeData node = createNavigation(createSite(SiteType.PORTAL, "update_rename"));
        createNodeChild(node, "foo");
        sync(true);

        //
        NavigationContext navigation = getNavigationService().loadNavigation(SiteKey.portal("update_rename"));
        NavigationNode root1 = getNavigationService().loadNode(NavigationNode.MODEL, navigation, Scope.CHILDREN, null).getNode();

        //
        NavigationNode root2 = getNavigationService().loadNode(NavigationNode.MODEL, navigation, Scope.CHILDREN, null).getNode();
        root2.getChild("foo").setName("bar");
        getNavigationService().saveNode(root2.getContext(), null);
        sync(true);

        //
        Iterator<NodeChange<NavigationNode, NodeState>> it = root1.update(getNavigationService(), null);
        NavigationNode bar = root1.getChild(0);
        assertEquals("bar", bar.getName());
        NodeChange.Renamed<NavigationNode, NodeState> renamed = (NodeChange.Renamed<NavigationNode, NodeState>) it.next();
        assertEquals("bar", renamed.getName());
        assertSame(bar, renamed.getTarget());
    }

    public void testState() throws NullPointerException, NavigationServiceException {
        NodeData node = createNavigation(createSite(SiteType.PORTAL, "update_state"));
        createNodeChild(createNodeChild(node, "foo")[0], "bar");
        sync(true);

        //
        NavigationContext navigation = getNavigationService().loadNavigation(SiteKey.portal("update_state"));
        NavigationNode root1 = getNavigationService().loadNode(NavigationNode.MODEL, navigation, Scope.CHILDREN, null).getNode();
        NavigationNode root2 = getNavigationService().loadNode(NavigationNode.MODEL, navigation, Scope.CHILDREN, null).getNode();
        NavigationNode root3 = getNavigationService().loadNode(NavigationNode.MODEL, navigation, Scope.GRANDCHILDREN, null).getNode();

        //
        NavigationNode root = getNavigationService().loadNode(NavigationNode.MODEL, navigation, Scope.CHILDREN, null).getNode();
        root.getChild("foo").setState(new NodeState.Builder().label("foo").build());
        getNavigationService().saveNode(root.getContext(), null);
        sync(true);

        //
        Iterator<NodeChange<NavigationNode, NodeState>> changes = root1.update(getNavigationService(), Scope.GRANDCHILDREN);
        NavigationNode foo = root1.getChild("foo");
        assertEquals("foo", foo.getState().getLabel());
        NodeChange.Added<NavigationNode, NodeState> added = (NodeChange.Added<NavigationNode, NodeState>) changes.next();
        assertEquals("bar", added.getTarget().getName());
        assertEquals(null, added.getPrevious());
        assertEquals("bar", added.getTarget().getName());
        NodeChange.Updated<NavigationNode, NodeState> updated = (NodeChange.Updated<NavigationNode, NodeState>) changes.next();
        assertSame(foo, updated.getTarget());
        assertEquals(new NodeState.Builder().label("foo").build(), updated.getState());
        assertFalse(changes.hasNext());

        //
        changes = root2.update(getNavigationService(), null);
        foo = root2.getChild("foo");
        assertEquals("foo", foo.getState().getLabel());
        updated = (NodeChange.Updated<NavigationNode, NodeState>) changes.next();
        assertSame(foo, updated.getTarget());
        assertEquals(new NodeState.Builder().label("foo").build(), updated.getState());
        assertFalse(changes.hasNext());

        //
        changes = root3.update(getNavigationService(), null);
        foo = root3.getChild("foo");
        assertEquals("foo", foo.getState().getLabel());
        updated = (NodeChange.Updated<NavigationNode, NodeState>) changes.next();
        assertSame(foo, updated.getTarget());
        assertEquals(new NodeState.Builder().label("foo").build(), updated.getState());
        assertFalse(changes.hasNext());
    }

    public void testUseMostActualChildren() throws NullPointerException, NavigationServiceException {
        NodeData node = createNavigation(createSite(SiteType.PORTAL, "update_with_most_actual_children"));
        createNodeChild(createNodeChild(node, "foo")[0], "bar");
        sync(true);

        //
        NavigationContext navigation = getNavigationService().loadNavigation(SiteKey.portal("update_with_most_actual_children"));
        NavigationNode root = getNavigationService().loadNode(NavigationNode.MODEL, navigation, Scope.CHILDREN, null).getNode();
        NavigationNode foo = root.getChild("foo");
        sync(true);

        //
        NavigationNode root1 = getNavigationService().loadNode(NavigationNode.MODEL, navigation, Scope.ALL, null).getNode();
        root1.getChild("foo").removeChild("bar");
        getNavigationService().saveNode(root1.getContext(), null);
        sync(true);

        //
        foo.update(getNavigationService(), Scope.CHILDREN);
        assertNull(foo.getChild("bar"));

        // Update a second time (it actually test a previous bug)
        foo.update(getNavigationService(), Scope.CHILDREN);
        assertNull(foo.getChild("bar"));
    }

    public void testUpdateDeletedNode() throws NullPointerException, NavigationServiceException {
        NodeData node = createNavigation(createSite(SiteType.PORTAL, "update_deleted_node"));
        createNodeChild(createNodeChild(node, "foo")[0], "bar");
        sync(true);

        //
        NavigationContext navigation = getNavigationService().loadNavigation(SiteKey.portal("update_deleted_node"));
        NavigationNode root = getNavigationService().loadNode(NavigationNode.MODEL, navigation, Scope.ALL, null).getNode();
        NavigationNode bar = root.getChild("foo").getChild("bar");
        sync(true);

        //
        NavigationNode root1 = getNavigationService().loadNode(NavigationNode.MODEL, navigation, Scope.ALL, null).getNode();
        root1.getChild("foo").removeChild("bar");
        getNavigationService().saveNode(root1.getContext(), null);
        sync(true);

        //
        Iterator<NodeChange<NavigationNode, NodeState>> changes = bar.update(getNavigationService(), Scope.CHILDREN);
        NodeChange.Removed<NavigationNode, NodeState> removed = (NodeChange.Removed<NavigationNode, NodeState>) changes.next();
        assertSame(bar, removed.getTarget());
        assertFalse(changes.hasNext());
    }

    public void testLoadEvents() throws Exception {
        NodeData node = createNavigation(createSite(SiteType.PORTAL, "update_load_events"));
        createNodeChild(createNodeChild(node, "foo")[0], "bar1", "bar2");
        sync(true);

        //
        NavigationContext navigation = getNavigationService().loadNavigation(SiteKey.portal("update_load_events"));
        NavigationNode root = getNavigationService().loadNode(NavigationNode.MODEL, navigation, Scope.SINGLE, null).getNode();

        //
        Iterator<NodeChange<NavigationNode, NodeState>> changes = root.update(getNavigationService(), Scope.ALL);

        //
        NavigationNode foo = root.getChild(0);
        assertEquals("foo", foo.getName());
        NavigationNode bar1 = foo.getChild(0);
        assertEquals("bar1", bar1.getName());
        NavigationNode bar2 = foo.getChild(1);
        assertEquals("bar2", bar2.getName());

        //
        NodeChange.Added<NavigationNode, NodeState> added1 = (NodeChange.Added<NavigationNode, NodeState>) changes.next();
        assertSame(foo, added1.getTarget());
        NodeChange.Added<NavigationNode, NodeState> added2 = (NodeChange.Added<NavigationNode, NodeState>) changes.next();
        assertSame(bar1, added2.getTarget());
        NodeChange.Added<NavigationNode, NodeState> added3 = (NodeChange.Added<NavigationNode, NodeState>) changes.next();
        assertSame(bar2, added3.getTarget());
        assertFalse(changes.hasNext());
    }

    public void testUpdateTwice2() throws NullPointerException, NavigationServiceException {
        NodeData node = createNavigation(createSite(SiteType.PORTAL, "update_twice2"));
        createNodeChild(createNodeChild(node, "foo")[0], "bar");
        sync(true);

        // Browser 1 : Expand the "foo" node
        NavigationContext navigation = getNavigationService().loadNavigation(SiteKey.portal("update_twice2"));
        NavigationNode root = getNavigationService().loadNode(NavigationNode.MODEL, navigation, Scope.CHILDREN, null).getNode();
        NavigationNode foo = root.getChild("foo");
        // If this line is commented, the test is passed
        getNavigationService().updateNode(foo.getContext(), Scope.CHILDREN, null);
        sync(true);

        // Browser 2: Change the "foo" node
        NavigationNode root1 = getNavigationService().loadNode(NavigationNode.MODEL, navigation, Scope.ALL, null).getNode();
        root1.getChild("foo").removeChild("bar");
        getNavigationService().saveNode(root1.getContext(), null);
        sync(true);

        // Browser 1: Try to expand the "foo" node 2 times ---> NPE after the 2nd updateNode method
        getNavigationService().updateNode(foo.getContext(), Scope.CHILDREN, null);
        getNavigationService().updateNode(foo.getContext(), Scope.CHILDREN, null);
    }

    public void testMove2() throws Exception {
        NodeData node = createNavigation(createSite(SiteType.PORTAL, "update_move2"));
        createNodeChild(createNodeChild(node, "a", "c")[0], "b");

        //
        sync(true);

        //
        NavigationContext navigation = getNavigationService().loadNavigation(SiteKey.portal("update_move2"));
        NodeContext<NavigationNode, NodeState> root = getNavigationService().loadNode(NavigationNode.MODEL, navigation, Scope.ALL, null);
        NavigationNode a = root.getNode("a");
        NavigationNode b = a.getChild("b");
        NavigationNode c = root.getNode("c");

        // Browser 2 : move the node "b" from "a" to "c"
        NodeContext<NavigationNode, NodeState> root1 = getNavigationService().loadNode(NavigationNode.MODEL, navigation, Scope.ALL, null);
        root1.getNode("c").addChild(root1.getNode("a").getChild("b"));
        getNavigationService().saveNode(root1.getNode().getContext(), null);
        //
        sync(true);

        // Browser 1: need NodeChange event to update UI
        NodeChangeQueue<NodeContext<NavigationNode, NodeState>, NodeState> queue = new NodeChangeQueue<NodeContext<NavigationNode, NodeState>, NodeState>();
        // If update "root1" --> NodeChange.Moved --> ok
        // If update "b" --> NodeChange.Add --> ok
        // update "a" --> no NodeChange, we need an event here (NodeChange.Remove) so UI can be updated
        getNavigationService().updateNode(a.getContext(), Scope.CHILDREN, queue);
        Iterator<NodeChange<NodeContext<NavigationNode, NodeState>, NodeState>> changes = queue.iterator();
        assertTrue(changes.hasNext());
    }

    public void testScope() throws Exception {
        NodeData node = createNavigation(createSite(SiteType.PORTAL, "update_scope"));
        NodeData[] ac = createNodeChild(node, "a", "c");
        createNodeChild(ac[0], "b");
        createNodeChild(ac[1], "d");

        //
        sync(true);

        //
        NavigationContext navigation = getNavigationService().loadNavigation(SiteKey.portal("update_scope"));
        NavigationNode root1 = getNavigationService().loadNode(NavigationNode.MODEL, navigation, Scope.CHILDREN, null).getNode();
        NavigationNode a = root1.getChild("a");
        NavigationNode c = root1.getChild("c");
        assertFalse(a.getContext().isExpanded());
        assertFalse(c.getContext().isExpanded());

        //
        NavigationNode root2 = getNavigationService().loadNode(NavigationNode.MODEL, navigation, Scope.ALL, null).getNode();
        root2.addChild("e");
        getNavigationService().saveNode(root2.getContext(), null);

        //
        sync(true);

        //
        getNavigationService().updateNode(a.getContext(), Scope.CHILDREN, null);
        assertSame(a, root1.getChild("a"));
        assertSame(c, root1.getChild("c"));
        assertNotNull(root1.getChild("e"));
        assertTrue(a.getContext().isExpanded());
        assertFalse(c.getContext().isExpanded());
        assertNotNull(a.getChild("b"));
    }

    public void _testPendingChange() throws NullPointerException, NavigationServiceException {
        NodeData node = createNavigation(createSite(SiteType.PORTAL, "update_pending_change"));
        createNodeChild(node, "foo", "bar");

        //
        sync(true);

        NavigationContext navigation = getNavigationService().loadNavigation(SiteKey.portal("update_pending_change"));
        NavigationNode root = getNavigationService().loadNode(NavigationNode.MODEL, navigation, Scope.CHILDREN, null).getNode();
        NavigationNode foo = root.getChild("foo");
        NavigationNode bar = root.getChild("bar");

        // Expand and change the "bar" node
        getNavigationService().updateNode(bar.getContext(), Scope.CHILDREN, null);
        bar.addChild("juu");

        // ---> IllegalArgumentException
        // Can't expand the "foo" node, even it doesn't have any pending changes
        getNavigationService().updateNode(foo.getContext(), Scope.CHILDREN, null);
    }

    public void testRemovedNavigation() throws Exception {
        createNavigation(createSite(SiteType.PORTAL, "update_removed_navigation"));

        //
        sync(true);

        //
        NavigationContext navigation = getNavigationService().loadNavigation(SiteKey.portal("update_removed_navigation"));
        NavigationNode root = getNavigationService().loadNode(NavigationNode.MODEL, navigation, Scope.ALL, null).getNode();
        getNavigationService().destroyNavigation(navigation);

        //
        sync(true);

        //
        try {
            getNavigationService().updateNode(root.getContext(), null, null);
        } catch (HierarchyException e) {
            assertSame(HierarchyError.UPDATE_CONCURRENTLY_REMOVED_NODE, e.getError());
        }
    }
}
