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
package de.dh.cad.architect.utils;

import java.util.Optional;
import java.util.function.Consumer;

public class CircularLinkedList<T> {
    public static class Node<T> {
        protected T mValue;
        protected Node<T> mPrevNode;
        protected Node<T> mNextNode;

        public Node(T value) {
            mValue = value;
        }

        public T getValue() {
            return mValue;
        }

        public Node<T> getPrevNode() {
            return mPrevNode;
        }

        public Node<T> getNextNode() {
            return mNextNode;
        }

        @Override
        public String toString() {
            return mValue.toString();
        }
    }

    private Node<T> mHead = null;

    public void addNode(T value) {
        Node<T> newNode = new Node<>(value);

        if (mHead == null) {
            mHead = newNode;
            mHead.mNextNode = newNode;
            mHead.mPrevNode = newNode;
        } else {
            mHead.mPrevNode.mNextNode = newNode;
            newNode.mPrevNode = mHead.mPrevNode;
            newNode.mNextNode = mHead;
            mHead.mPrevNode = newNode;
        }
    }

    public void deleteNode(T value) {
        if (mHead == null) {
            return;
        }
        Node<T> currentNode = mHead;
        do {
            if (currentNode.mValue.equals(value)) {
                if (mHead.mNextNode == mHead) {
                    mHead = null;
                } else {
                    if (currentNode == mHead) {
                        mHead = currentNode.mNextNode;
                    }
                    currentNode.mPrevNode.mNextNode = currentNode.mNextNode;
                    currentNode.mNextNode.mPrevNode = currentNode.mPrevNode;
                }
                break;
            }
            currentNode = currentNode.mNextNode;
        } while (currentNode != mHead);
    }

    public Optional<Node<T>> findNode(T searchValue) {
        Node<T> currentNode = mHead;

        if (mHead != null) {
            do {
                if (currentNode.mValue.equals(searchValue)) {
                    return Optional.of(currentNode);
                }
                currentNode = currentNode.mNextNode;
            } while (currentNode != mHead);
        }
        return Optional.empty();
    }

    public void forEach(Consumer<T> c) {
        Node<T> currentNode = mHead;

        if (mHead != null) {
            do {
                c.accept(currentNode.mValue);
                currentNode = currentNode.mNextNode;
            } while (currentNode != mHead);
        }
    }
}
