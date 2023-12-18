// This small library is meant to be a lightweight replacement of jQuery basic functions.

window.$ = document.querySelectorAll.bind(document);
Node.prototype.on = window.on = function (eventNames, fn) {
  let events = eventNames.split(' ')
  for (let i = 0; i < events.length; ++i) {
    let name = events[i];
    this.addEventListener(name, fn);
  }
  return this;
};
NodeList.prototype.__proto__ = Array.prototype;
NodeList.prototype.on = NodeList.prototype.addEventListener = function (eventNames, fn) {
  this.forEach(function (elem, i) {
    elem.on(eventNames, fn);
  });
  return this;
}
NodeList.prototype.addClass = function(className) {
  this.forEach(function (elem, i) {
    elem.classList.add(className);
  });
  return this;
}
Element.prototype.addClass = function(className) {
  this.classList.add(className);
  return this;
}
NodeList.prototype.removeClass = function(className) {
  this.forEach(function (elem, i) {
    elem.classList.remove(className);
  });
  return this;
}
Element.prototype.removeClass = function(className) {
  this.classList.remove(className);
  return this;
}
NodeList.prototype.toggleClass = function(className) {
  this.forEach(function (elem, i) {
    elem.classList.toggle(className);
  });
  return this;
}
Element.prototype.toggleClass = function(className) {
  this.classList.toggle(className);
  return this;
}
NodeList.prototype.hasClass = function(className) {
  console.log('nodelist.hasClass')
  console.log(this.item(0));
  return this.item(0).classList.contains(className);
}
Element.prototype.hasClass = function(className) {
  console.log('element.hasClass')
  console.log(this.classList)
  console.log(this.classList.contains(className))
  return this.classList.contains(className);
}
Node.prototype.offset = function() {
  let _x = 0;
  let _y = 0;
  let el = this;
  while( el && !isNaN( el.offsetLeft ) && !isNaN( el.offsetTop ) ) {
    _x += el.offsetLeft - el.scrollLeft;
    _y += el.offsetTop - el.scrollTop;
    el = el.offsetParent;
  }
  return { top: _y, left: _x };
}
NodeList.prototype.offset = function() {
  this.item(0).offset();
}
Element.prototype.attr = function (key) {
  return this.attributes[key].value;
}
NodeList.prototype.attr = function(key) {
  this.item(0).attr(key);
}
Element.prototype.data = function (key) {
  return this.attributes[`data-${key}`].value
}
NodeList.prototype.data = function(key) {
  this.item(0).data(key);
}
NodeList.prototype.show = function() {
  this.item(0).show();
  return this;
}
Element.prototype.show = function() {
  this.style.display = 'block';
}
NodeList.prototype.hide = function() {
  this.item(0).hide();
  return this;
}
Element.prototype.hide = function() {
  this.style.display = 'none';
}
NodeList.prototype.text = function(txt) {
  this.item(0).text(txt);
}
Element.prototype.text = function(txt) {
  if (typeof(txt) === 'undefined') {
    return this.textContent;
  } else {
    this.textContent = txt;
  }
}
NodeList.prototype.item = function (i) {
  return this[+i || 0];
};
NodeList.prototype.find = function(selector) {
  let result = [];
  this.forEach(function (elem, i) {
    let partial = elem.find(selector);
    result = result.concat([...partial]);
  });
  return Reflect.construct(Array, result, NodeList);
}
Element.prototype.find = function(selector) {
  return this.querySelectorAll(':scope ' + selector);
}

NodeList.prototype.clear = function() {
  this.forEach(function (elem, i) {
    elem.clear();
  });
  return this;
}
Element.prototype.clear = function() {
  this.innerHTML = '';
  return this;
}

/*
 TODO - conflicts with from.val(), rename one of the two
NodeList.prototype.val = function(value) {
  this.item(0).val(value);
}
Element.prototype.val = function(value) {
  // TODO - check that "this" has the "value" property
  if (typeof(value) === 'undefined') {
    return this.value;
  } else {
    this.value = value;
  }
}
*/

NodeList.prototype.focus = function() {
  let first = this.item(0);
  if (first) first.focus();
}
