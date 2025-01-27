/*
 * Copyright (C) 2025 IO Club
 *
 * This file is part of Workyras.
 *
 * Workyras is free software: you can redistribute it and/or modify
 * it under the terms of the Lesser GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Workyras is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * Lesser GNU General Public License for more details.
 *
 * You should have received a copy of the Lesser GNU General Public License
 * along with Workyras.  If not, see <https://www.gnu.org/licenses/>.
 */

package fyi.ioclub.workyras.models

sealed interface DisplayWorkTag {

    val id: ByteArray

    val name: String
}

sealed interface WorkTag : DisplayWorkTag {

    val link: Link

    sealed interface Link {

        val prev: WorkTag
        val next: WorkTag

        operator fun component1() = prev
        operator fun component2() = next

        companion object {
            val Unbound: Link = Mutable.Link.Unbound
        }
    }

    sealed interface Cloneable : WorkTag {
        val copy: WorkTag
    }

    sealed interface Mutable : WorkTag {

        override var name: String

        override var link: Link

        /** Link for [WorkTag.Mutable] */
        sealed interface Link : WorkTag.Link {

            override val prev: Mutable
            override val next: Mutable

            /** Deleted between [prev] and [next]. */
            fun removed()

            class Impl(
                override val prev: Mutable,
                override val next: Mutable,
            ) : Link {

                override fun removed() {
                    prev.link = Impl(prev = prev.link.prev, next = next)
                    next.link = Impl(prev = prev, next = next.link.next)
                }

                override fun toString() = "Link(prev=$prev, next=$next)"
            }

            object Unbound : Link {

                override val prev: Mutable get() = throw UnsupportedOperationException("Prev unbound")
                override val next: Mutable get() = throw UnsupportedOperationException("Next unbound")

                override fun removed() = throw UnsupportedOperationException("No bound to remove")

                override fun toString() = "Unbound Link"
            }
        }

        fun insertedBefore(target: Mutable)
        fun insertedAfter(target: Mutable)

        class Impl private constructor(
            override val id: ByteArray,
            override var name: String,
            private var _link: Link,
        ) : Mutable, Cloneable {

            override var link
                get() = _link
                set(link) = link.run {
                    if (this !== Link.Unbound)
                        when (this@Impl) {
                            prev, next -> throw IllegalArgumentException("Circled link")
                        }
                    let(::_link::set)
                }

            constructor(
                id: ByteArray,
                name: String,
            ) : this(id, name, Link.Unbound)

            override fun insertedBefore(target: Mutable) {
                // Update relative position info of [this]
                link = Link.Impl(prev = target.link.prev, next = target)

                val curr = this
                // Insert [curr] between [target.link.prev] and [target]
                target.run {
                    link.prev.run { link = Link.Impl(prev = link.prev, next = curr) }
                    link = Link.Impl(prev = curr, next = link.next)
                }
            }

            override fun insertedAfter(target: Mutable) {
                // Update relative position info of [this]
                link = Link.Impl(prev = target, next = target.link.next)

                val curr = this
                // Insert [curr] between [target] and [target.link.next]
                target.run {
                    link.next.run { link = Link.Impl(prev = curr, next = link.next) }
                    link = Link.Impl(prev = link.prev, next = curr)
                }
            }

            override val copy get() = Impl(id, name, _link)

            override fun toString() = "User Work Tag $name"
        }

        object Root : Mutable {
            override val id get() = throw UnsupportedOperationException("Root has no ID")
            override var name: String
                get() = throw UnsupportedOperationException("Root has no name")
                set(_) = throw UnsupportedOperationException("Root has no name to set")

            override var link: Link = Link.Impl(this, this)

            override fun insertedBefore(target: Mutable) =
                throw UnsupportedOperationException("Cannot move root")

            override fun insertedAfter(target: Mutable) =
                throw UnsupportedOperationException("Cannot move root")

            override fun toString() = "Root Work Tag"
        }
    }

    companion object {
        val Root: WorkTag = Mutable.Root
    }
}
