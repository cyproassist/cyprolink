define([], function() {
	return function() {
		this.attributes({
			itemType : null,
			itemTemplate : null,
			itemVariable : null
		});

		/**
		 * RDFa 'query' for items.
		 */
		function createQuery(itemType, keywords) {
			var template = $('<div data-lift="rdfa"><div data-text="?text"></div><div about="?item" typeof="' + itemType + '" class="clearable">' + //
			'<div property="http://linkedfactory.org/vocab/maintenance#text" content="?text" class="asc"></div>' + //
			'<div class="search-patterns" data-for="?text"></div></div>');
			template.find(".search-patterns").attr("data-value", keywords);
			return $('<div>').append(template).html();
		}

		/**
		 * Computes value proposals for typeaheadjs.
		 */
		this.computeProposals = function(query, cb) {
			enilink.render({
				what : createQuery(this.attr.itemType, query)
			}, {
				model : enilink.param("model")
			}, function(html) {
				var items = $(html).find("[data-text]").map(function() {
					var v = $(this).attr("data-text");
					return {
						label : v,
						content : v
					};
				}).get();
				cb(items);
			});
		}

		/**
		 * Creates a new item and inserts it into the list.
		 */
		this.createItem = function(value) {
			var component = this;

			var d = new $.Deferred;
			enilink.rdf.updateTriples({
				"new:item-" : {
					"http://www.w3.org/1999/02/22-rdf-syntax-ns#type" : [ {
						value : component.attr.itemType,
						type : "uri"
					} ],
					"http://linkedfactory.org/vocab/maintenance#text" : [ {
						value : value,
						type : "literal",
						lang : enilink.param("lang") || "de"
					} ]
				}
			}, function(uriMap) {
				var bindParams = {
						currentLang : enilink.param("lang") || "de"
				};
				bindParams[component.attr.itemVariable] = uriMap['new:item-'];

				enilink.render({
					template : component.attr.itemTemplate,
					bind : bindParams
				}, {
					model : enilink.contextModel(this)
				}, function(html) {
					var newItem = $(html);
					newItem.appendTo(component.$node.find('ul'));
					component.addHandlers(newItem);
					component.updateOrder(newItem.prev(), newItem);
					d.resolve();
				});
				return false;
			});

			return d.promise();
		}
	};
});