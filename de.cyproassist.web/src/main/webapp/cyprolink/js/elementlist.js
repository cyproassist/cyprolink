define([ "flight/lib/component", "flight/lib/utils" ], function(defineComponent, utils, d3, simplify) {
	return defineComponent(function() {
		this.attributes({
			nextSel : '.next[rel]',
			nextTpl : '<span style="display:none" class="next">'
		});

		/**
		 * Simple topological sort implementation for sorting the element list.
		 */
		function toposort(edges) {
			var nodes = {}, // hash: stringified node id => node
			sorted = [], // sorted list of IDs ( returned value )
			visited = {}; // hash: id of already visited node => true

			var Node = function(id) {
				this.id = id;
				this.successors = [];
			}

			// 1. build data structures
			edges.forEach(function(v) {
				var from = v[0], to = v[1];
				if (!nodes[from])
					nodes[from] = new Node(from);
				if (!nodes[to])
					nodes[to] = new Node(to);
				nodes[from].successors.push(to);
			});

			// 2. topological sort
			Object.keys(nodes).forEach(function visit(idstr, ancestors) {
				var node = nodes[idstr], id = node.id;

				// if already exists, do nothing
				if (visited[idstr]) {
					return;
				}

				if (!Array.isArray(ancestors)) {
					ancestors = [];
				}

				ancestors.push(id);
				visited[idstr] = true;

				node.successors.forEach(function(afterID) {
					if (ancestors.indexOf(afterID) >= 0) // if
						// already in ancestors, a closed chain exists.
						if (window.console) {
							console.log('Closed chain: ' + afterID + ' is already predecessor of ' + id);
							return;
						}
					// recursive call
					visit(afterID.toString(), ancestors.slice());
				});

				sorted.unshift(id);
			});

			return sorted;
		}

		/**
		 * Updates the ordering relationships/properties between the given DOM
		 * nodes according to their current order in the DOM tree.
		 */
		this.updateOrder = function() {
			var rdfBefore = $.rdf.databank();
			$.each(arguments, function(i, elem) {
				elem.rdf().databank.triples().get().forEach(function(triple) {
					rdfBefore.add(triple);
				});
			});

			var nextSel = this.attr.nextSel;
			var nextTpl = this.attr.nextTpl;

			var rdfAfter = $.rdf.databank();
			// update RDFa
			$.each(arguments, function(i, elem) {
				// remove old precedence statement
				elem.find(nextSel).remove();

				var succResource = elem.next().attr("about");
				if (succResource) {
					// add new precedence statement
					var nextRel = elem.closest("[data-next-rel]").attr("data-next-rel");
					$(nextTpl).attr("rel", nextRel).attr("resource", succResource).prependTo(elem);
				}

				elem.rdf().databank.triples().get().forEach(function(triple) {
					rdfAfter.add(triple);
				});
			});

			var added = rdfAfter.except(rdfBefore).dump();
			var removed = rdfBefore.except(rdfAfter).dump();
			enilink.rdf.updateTriples(added, removed, function(success) {
				return false;
			});
		}

		/**
		 * Creates an 'Add' button for new elements.
		 */
		this.createAddButton = function() {
			var component = this;

			var addBtn = this.$node.find('.add-element');
			var options = {
				type : "typeaheadjs",
				onblur : "ignore",
				inputclass : "editable-form-control",
				emptyclass : "",
				emptytext : "",
				mode : "inline",
				toggle : "manual",
				display : false
			};

			var timeoutID = null;
			options.typeahead = [ {
				minLength : 2
			}, {
				source : function(query, cb) {
					if (timeoutID) {
						clearTimeout(timeoutID);
					}
					timeoutID = window.setTimeout(function() {
						component.computeProposals(query, cb);
					}, 500);
				},
				displayKey : function(v) {
					return v.content;
				},
				templates : {
					suggestion : function(v) {
						var t = $('<p />').css("white-space", "nowrap").text(v.label);
						return $("<span />").append(t);
					}
				}
			} ];

			addBtn.editable($.extend(options, {
				url : function(params) {
					return component.createItem(params.value);
				}
			}));
			// manually trigger the add button
			addBtn.find('i').click(function() {
				addBtn.editable("show");
			});
		}

		/**
		 * Register click handlers for moving elements up and down.
		 */
		this.addHandlers = function(parent) {
			var component = this;
			function up() {
				var self = $(this).closest(".item");
				var pred = self.prev();
				if (pred.length && pred.is(':visible')) {
					var pred2 = pred.prev();
					self.slideUp(function() {
						self.insertBefore(pred);
						component.updateOrder(pred2, self, pred);
						self.slideDown();
					});
				}
			}
			
			function down() {
				var self = $(this).closest(".item");
				var succ = self.next();
				if (succ.length) {
					var pred = self.prev();
					self.slideUp(function() {
						self.insertAfter(succ);
						component.updateOrder(pred, succ, self);
						self.slideDown();
					});
				}
			}
			
			function remove() {
				var self = $(this).closest(".item");
				var resource = self.resourceAttr("about").value.toString();
				enilink.rdf.removeResource(resource, function (success) {
					if (success) {
						var prev = self.prev();
						var next = self.next();
						self.remove();
						component.updateOrder(prev, next);
					}
				});
			}

			parent.find(".glyphicon-arrow-up").closest("a").click(up);
			parent.find(".glyphicon-arrow-down").closest("a").click(down);
			parent.find(".glyphicon-trash").closest("a").click(remove);
		}

		this.after('initialize', function() {
			var nextSel = this.attr.nextSel;
			var nextTpl = this.attr.nextTpl;

			var elements = this.$node.find('.items-list').children();
			elements.addClass('item');
			
			// determine partial ordering
			var edges = elements.map(function() {
				var self = $(this);
				var uri = self.attr("resource") || self.attr("about");
				return self.find(nextSel).map(function() {
					var succ = $(this).attr("resource") || $(this).attr("about");
					return [ [ uri, succ ] ];
				}).get();
			}).get();

			// sort elements
			var prev = null;
			$.each(toposort(edges), function(index, value) {
				var element = elements.filter('[resource="' + value + '"],[about="' + value + '"]');
				if (prev) {
					element.insertAfter(prev);
				}
				if (element.length) {
					prev = element;
				}
			});

			this.addHandlers(this.$node);
			this.createAddButton();
		});
	});
});