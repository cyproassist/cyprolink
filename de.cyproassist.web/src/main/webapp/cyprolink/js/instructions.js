define([ "flight/lib/component", "flight/lib/utils" ], function(
		defineComponent, utils, d3, simplify) {
	return defineComponent(function() {
		this.attributes({
			orderRel : null
		});

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
			Object.keys(nodes).forEach(
					function visit(idstr, ancestors) {
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
									console.log('Closed chain: ' + afterID
											+ ' is already predecessor of '
											+ id);
									return;
								}
							// recursive call
							visit(afterID.toString(), ancestors.slice());
						});

						sorted.unshift(id);
					});

			return sorted;
		}

		this.updateOrder = function() {
			var precedesSel = this.precedesSel;
			var precedesTpl = this.precedesTpl;

			var rdfBefore = $.rdf.databank();
			$.each(arguments, function(i, elem) {
				elem.rdf().databank.triples().get().forEach(function(triple) {
					rdfBefore.add(triple);
				});
			});

			var rdfAfter = $.rdf.databank();
			// update RDFa
			$.each(arguments, function(i, elem) {
				// remove old precedence statement
				elem.find(precedesSel).remove();

				var succResource = elem.next().attr("about");
				if (succResource) {
					// add new precedence statement
					$(precedesTpl).attr("resource", succResource).prependTo(
							elem);
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

		this.after('initialize', function() {
			var precedesSel = this.precedesSel = '[rel="' + this.attr.orderRel
					+ '"]';
			var precedesTpl = this.precedesTpl = '<span rel="'
					+ this.attr.orderRel + '" style="display:none">';

			var elements = this.$node.find("li");
			// determine partial ordering
			var edges = elements.map(
					function() {
						var self = $(this);
						var uri = self.attr("resource") || self.attr("about");
						return self.find(precedesSel).map(
								function() {
									var succ = $(this).attr("resource")
											|| $(this).attr("about");
									return [ [ uri, succ ] ];
								}).get();
					}).get();

			// sort elements
			var prev = null;
			$.each(toposort(edges), function(index, value) {
				var element = elements.filter('[resource="' + value
						+ '"],[about="' + value + '"]');
				if (prev) {
					element.insertAfter(prev);
				}
				if (element.length) {
					prev = element;
				}
			});

			// register click handlers for up/down actions
			function addHandlers(parent) {
				parent.find(".glyphicon-arrow-up").closest("a").click(up);
				parent.find(".glyphicon-arrow-down").closest("a").click(down);
			}
			addHandlers(this.$node);

			var component = this;

			function up() {
				var self = $(this).closest("li");
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
				var self = $(this).closest("li");
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

			// overrides default onadd behavior to insert the nodes in a
			// certain positions as well as to register event handlers
			function onadd(response) {
				if (response.html) {
					var newNode = $(response.html);
					// update precedence relationship
					var pred = $(".processes li").last();
					if (pred.length > 0) {
						$(precedesTpl).attr("resource", newNode.attr("about"))
								.prependTo(pred);
					}
					// insert node and register event handlers
					$(".processes").append(newNode);
					enilink.ui.enableEdit(newNode);
					addHandlers(newNode);
					return newNode;
				}
			}
		});
	});
});